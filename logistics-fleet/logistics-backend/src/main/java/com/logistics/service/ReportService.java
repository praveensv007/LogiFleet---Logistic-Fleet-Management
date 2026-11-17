package com.logistics.service;

import com.logistics.dto.response.DashboardSummary;
import com.logistics.entity.enums.ShipmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    /** Filters from the Reports screen. Any field may be null. */
    record ReportFilter(LocalDate from, LocalDate to, Long warehouseId, Long driverId, Long vehicleId,
                        ShipmentStatus status) {
        public LocalDate fromOrDefault() {
            return from != null ? from : LocalDate.now().minusDays(29);
        }

        public LocalDate toOrDefault() {
            return to != null ? to : LocalDate.now();
        }
    }

    record ExportFile(String fileName, String contentType, byte[] content) {
    }

    DashboardSummary summary();

    List<Map<String, Object>> shipmentsByDay(ReportFilter f);

    List<Map<String, Object>> onTime(ReportFilter f);

    List<Map<String, Object>> driverPerformance(ReportFilter f);

    List<Map<String, Object>> vehicleUtilisation(ReportFilter f);

    List<Map<String, Object>> maintenanceCost(ReportFilter f);

    List<Map<String, Object>> pendingByWarehouse(ReportFilter f);

    /** report = shipments-by-day | on-time | driver-performance | vehicle-utilisation | maintenance-cost | pending-by-warehouse */
    ExportFile export(String report, String type, ReportFilter f);
}
