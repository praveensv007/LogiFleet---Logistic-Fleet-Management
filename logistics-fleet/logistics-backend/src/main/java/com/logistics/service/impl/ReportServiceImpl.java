package com.logistics.service.impl;

import com.logistics.dto.response.DashboardSummary;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.repository.*;
import com.logistics.service.ReportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final EnumSet<AssignmentStatus> ACTIVE = EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentStatusHistoryRepository historyRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryProblemRepository problemRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMaintenanceRepository maintenanceRepository;
    private final FleetAssignmentRepository assignmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardSummary summary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (ShipmentStatus st : ShipmentStatus.values()) {
            breakdown.put(st.name(), shipmentRepository.countByCurrentStatus(st));
        }
        return DashboardSummary.builder()
                .totalShipments(shipmentRepository.count())
                .shipmentsToday(shipmentRepository.countByCreatedAtBetween(start, end))
                .deliveredToday(historyRepository.countByToStatusAndChangedAtBetween(ShipmentStatus.DELIVERED, start, end))
                .pendingShipments(breakdown.get(ShipmentStatus.CREATED.name()))
                .inWarehouse(shipmentRepository.countByCurrentStatusIn(ShipmentStatus.WAREHOUSE_QUEUE))
                .onRoad(shipmentRepository.countByCurrentStatusIn(ShipmentStatus.ON_ROAD))
                .delivered(breakdown.get(ShipmentStatus.DELIVERED.name()))
                .cancelled(breakdown.get(ShipmentStatus.CANCELLED.name()))
                .activeDeliveries(deliveryRepository.findByAssignmentStatusIn(ACTIVE).size())
                .delayedDeliveries(deliveryRepository.countDelayedActive(ACTIVE))
                .openProblems(problemRepository.countByResolutionStatusNot(ResolutionStatus.RESOLVED))
                .availableDrivers(driverRepository.countByAvailabilityStatus(DriverAvailability.AVAILABLE))
                .driversOnTrip(driverRepository.countByAvailabilityStatus(DriverAvailability.ON_TRIP))
                .availableVehicles(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE))
                .vehiclesOnTrip(vehicleRepository.countByStatus(VehicleStatus.ON_TRIP))
                .vehiclesInMaintenance(vehicleRepository.countByStatus(VehicleStatus.UNDER_MAINTENANCE))
                .maintenanceDue(maintenanceRepository.findByStatusInAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
                        EnumSet.of(MaintenanceStatus.SCHEDULED), LocalDate.now().plusDays(7)).size())
                .totalCustomers(customerRepository.count())
                .totalWarehouses(warehouseRepository.count())
                .totalUsers(userRepository.count())
                .statusBreakdown(breakdown)
                .build();
    }

    @Override
    public List<Map<String, Object>> shipmentsByDay(ReportFilter f) {
        LocalDate from = f.fromOrDefault();
        LocalDate to = f.toOrDefault();
        checkRange(from, to);
        List<Shipment> created = shipmentRepository.findByCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().filter(s -> matchesWarehouse(s, f.warehouseId()))
                .filter(s -> f.status() == null || s.getCurrentStatus() == f.status())
                .toList();
        Map<LocalDate, Long> createdByDay = created.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> deliveredByDay = historyRepository.findByToStatusAndChangedAtBetween(
                        ShipmentStatus.DELIVERED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().filter(h -> matchesWarehouse(h.getShipment(), f.warehouseId()))
                .collect(Collectors.groupingBy(h -> h.getChangedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> cancelledByDay = historyRepository.findByToStatusAndChangedAtBetween(
                        ShipmentStatus.CANCELLED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().filter(h -> matchesWarehouse(h.getShipment(), f.warehouseId()))
                .collect(Collectors.groupingBy(h -> h.getChangedAt().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("created", createdByDay.getOrDefault(d, 0L));
            row.put("delivered", deliveredByDay.getOrDefault(d, 0L));
            row.put("cancelled", cancelledByDay.getOrDefault(d, 0L));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> onTime(ReportFilter f) {
        List<Delivery> delivered = deliveredInRange(f);
        long onTime = delivered.stream().filter(this::isOnTime).count();
        long total = delivered.size();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("from", f.fromOrDefault().toString());
        row.put("to", f.toOrDefault().toString());
        row.put("totalDelivered", total);
        row.put("onTime", onTime);
        row.put("delayed", total - onTime);
        row.put("onTimePercent", pct(onTime, total));
        row.put("delayedPercent", pct(total - onTime, total));
        return List.of(row);
    }

    @Override
    public List<Map<String, Object>> driverPerformance(ReportFilter f) {
        LocalDateTime from = f.fromOrDefault().atStartOfDay();
        LocalDateTime to = f.toOrDefault().plusDays(1).atStartOfDay();
        Map<Long, List<FleetAssignment>> byDriver = assignmentRepository.findByAssignedAtBetween(from, to).stream()
                .filter(a -> f.driverId() == null || a.getDriver().getId().equals(f.driverId()))
                .filter(a -> f.vehicleId() == null || a.getVehicle().getId().equals(f.vehicleId()))
                .filter(a -> matchesWarehouse(a.getShipment(), f.warehouseId()))
                .collect(Collectors.groupingBy(a -> a.getDriver().getId()));
        List<Driver> drivers = f.driverId() != null
                ? driverRepository.findById(f.driverId()).map(List::of).orElse(List.of())
                : driverRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Driver d : drivers) {
            List<FleetAssignment> list = byDriver.getOrDefault(d.getId(), List.of());
            long completed = list.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
            long rejected = list.stream().filter(a -> a.getStatus() == AssignmentStatus.REJECTED).count();
            long onTime = list.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED
                    && a.getDelivery() != null && isOnTime(a.getDelivery())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("driverCode", d.getDriverCode());
            row.put("driverName", d.getFullName());
            row.put("assigned", list.size());
            row.put("delivered", completed);
            row.put("rejected", rejected);
            row.put("onTime", onTime);
            row.put("delayed", completed - onTime);
            row.put("onTimePercent", pct(onTime, completed));
            row.put("problemsReported", problemRepository.countByDelivery_Driver_IdAndReportedAtBetween(d.getId(), from, to));
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (Long) r.get("delivered")).reversed());
        return rows;
    }

    @Override
    public List<Map<String, Object>> vehicleUtilisation(ReportFilter f) {
        LocalDateTime from = f.fromOrDefault().atStartOfDay();
        LocalDateTime to = f.toOrDefault().plusDays(1).atStartOfDay();
        double rangeHours = Duration.between(from, to).toMinutes() / 60.0;
        // trips that started before the range but are still running count too: look back 30 days
        Map<Long, List<FleetAssignment>> byVehicle = assignmentRepository.findByAssignedAtBetween(from.minusDays(30), to).stream()
                .filter(a -> a.getAcceptedAt() != null)
                .filter(a -> f.driverId() == null || a.getDriver().getId().equals(f.driverId()))
                .collect(Collectors.groupingBy(a -> a.getVehicle().getId()));
        List<Vehicle> vehicles = f.vehicleId() != null
                ? vehicleRepository.findById(f.vehicleId()).map(List::of).orElse(List.of())
                : vehicleRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (f.warehouseId() != null && (v.getHomeWarehouse() == null || !v.getHomeWarehouse().getId().equals(f.warehouseId()))) {
                continue;
            }
            double hours = 0;
            int trips = 0;
            for (FleetAssignment a : byVehicle.getOrDefault(v.getId(), List.of())) {
                LocalDateTime start = a.getAcceptedAt().isBefore(from) ? from : a.getAcceptedAt();
                LocalDateTime endRaw = a.getCompletedAt() != null ? a.getCompletedAt() : LocalDateTime.now();
                LocalDateTime end = endRaw.isAfter(to) ? to : endRaw;
                if (end.isAfter(start)) {
                    hours += Duration.between(start, end).toMinutes() / 60.0;
                    trips++;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleNumber", v.getVehicleNumber());
            row.put("vehicleType", v.getVehicleType().name());
            row.put("status", v.getStatus().name());
            row.put("trips", trips);
            row.put("hoursOnTrip", round(hours));
            row.put("utilisationPercent", rangeHours > 0 ? round(Math.min(100, hours * 100 / rangeHours)) : 0);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> maintenanceCost(ReportFilter f) {
        List<VehicleMaintenance> list = maintenanceRepository.findByStatusAndCompletedDateBetween(
                        MaintenanceStatus.COMPLETED, f.fromOrDefault(), f.toOrDefault()).stream()
                .filter(m -> f.vehicleId() == null || m.getVehicle().getId().equals(f.vehicleId()))
                .toList();
        Map<Vehicle, List<VehicleMaintenance>> byVehicle = list.stream()
                .collect(Collectors.groupingBy(VehicleMaintenance::getVehicle, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> rows = new ArrayList<>();
        byVehicle.forEach((v, items) -> {
            BigDecimal labour = items.stream().map(m -> nz(m.getLabourCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = items.stream().map(m -> nz(m.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleNumber", v.getVehicleNumber());
            row.put("services", items.size());
            row.put("labourCost", labour);
            row.put("partsCost", total.subtract(labour).max(BigDecimal.ZERO));
            row.put("totalCost", total);
            rows.add(row);
        });
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (BigDecimal) r.get("totalCost")).reversed());
        return rows;
    }

    @Override
    public List<Map<String, Object>> pendingByWarehouse(ReportFilter f) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Warehouse w : warehouseRepository.findAll()) {
            if (f.warehouseId() != null && !w.getId().equals(f.warehouseId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("warehouseCode", w.getWarehouseCode());
            row.put("warehouseName", w.getName());
            long total = 0;
            for (ShipmentStatus st : ShipmentStatus.WAREHOUSE_QUEUE) {
                long c = shipmentRepository.countByOriginWarehouse_IdAndCurrentStatusIn(w.getId(), List.of(st));
                row.put(st.name(), c);
                total += c;
            }
            row.put("totalPending", total);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public ExportFile export(String report, String type, ReportFilter f) {
        List<Map<String, Object>> rows = switch (report == null ? "" : report) {
            case "shipments-by-day" -> shipmentsByDay(f);
            case "on-time" -> onTime(f);
            case "driver-performance" -> driverPerformance(f);
            case "vehicle-utilisation" -> vehicleUtilisation(f);
            case "maintenance-cost" -> maintenanceCost(f);
            case "pending-by-warehouse" -> pendingByWarehouse(f);
            default -> throw new BadRequestException("report", "Unknown report: " + report);
        };
        String base = report + "-" + LocalDate.now();
        if ("pdf".equalsIgnoreCase(type)) {
            return new ExportFile(base + ".pdf", "application/pdf", toPdf(report, f, rows));
        }
        return new ExportFile(base + ".csv", "text/csv", toCsv(rows));
    }

    // ---------- helpers ----------

    private List<Delivery> deliveredInRange(ReportFilter f) {
        return deliveryRepository.findByDeliveredTimeBetween(
                        f.fromOrDefault().atStartOfDay(), f.toOrDefault().plusDays(1).atStartOfDay()).stream()
                .filter(d -> d.getCurrentStatus() == DeliveryStatus.DELIVERED)
                .filter(d -> f.driverId() == null || d.getDriver().getId().equals(f.driverId()))
                .filter(d -> f.vehicleId() == null || d.getAssignment().getVehicle().getId().equals(f.vehicleId()))
                .filter(d -> matchesWarehouse(d.getShipment(), f.warehouseId()))
                .toList();
    }

    private boolean isOnTime(Delivery d) {
        LocalDateTime expected = d.getAssignment().getExpectedDeliveryTime();
        return !d.isDelayed() && d.getDeliveredTime() != null
                && (expected == null || !d.getDeliveredTime().isAfter(expected));
    }

    private boolean matchesWarehouse(Shipment s, Long warehouseId) {
        return warehouseId == null || (s.getOriginWarehouse() != null && s.getOriginWarehouse().getId().equals(warehouseId));
    }

    private void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("to", "To date must be on or after From date");
        }
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() > 366) {
            throw new BadRequestException("to", "Choose a range of one year or less");
        }
    }

    private static double pct(long part, long total) {
        return total == 0 ? 0 : round(part * 100.0 / total);
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private byte[] toCsv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        if (!rows.isEmpty()) {
            sb.append(String.join(",", rows.get(0).keySet())).append('\n');
            for (Map<String, Object> row : rows) {
                sb.append(row.values().stream().map(this::csvCell).collect(Collectors.joining(","))).append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(Object v) {
        String s = v == null ? "" : v.toString();
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private byte[] toPdf(String report, ReportFilter f, List<Map<String, Object>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 9);
        doc.add(new Paragraph("Logistics & Fleet Management - " + report.replace('-', ' '), title));
        doc.add(new Paragraph("Period: " + f.fromOrDefault() + " to " + f.toOrDefault() + "   Generated: "
                + LocalDateTime.now().withNano(0), body));
        doc.add(new Paragraph(" "));
        if (rows.isEmpty()) {
            doc.add(new Paragraph("No data for the selected filters.", body));
        } else {
            List<String> cols = new ArrayList<>(rows.get(0).keySet());
            PdfPTable table = new PdfPTable(cols.size());
            table.setWidthPercentage(100);
            for (String c : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(c, head));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setGrayFill(0.9f);
                table.addCell(cell);
            }
            for (Map<String, Object> row : rows) {
                for (String c : cols) {
                    Object v = row.get(c);
                    table.addCell(new Phrase(v == null ? "" : v.toString(), body));
                }
            }
            doc.add(table);
        }
        doc.close();
        return out.toByteArray();
    }
}
