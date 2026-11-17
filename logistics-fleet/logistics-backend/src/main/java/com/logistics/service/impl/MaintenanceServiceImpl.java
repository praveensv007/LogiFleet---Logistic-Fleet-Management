package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.MaintenanceRequest;
import com.logistics.dto.response.MaintenanceResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.MaintenancePart;
import com.logistics.entity.Vehicle;
import com.logistics.entity.VehicleMaintenance;
import com.logistics.entity.enums.AuditAction;
import com.logistics.entity.enums.MaintenanceStatus;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.MasterDataMapper;
import com.logistics.repository.VehicleMaintenanceRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.service.MaintenanceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final VehicleMaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentStatusManager statusManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaintenanceResponse> list(Long vehicleId, MaintenanceStatus status, int page, int size, String sort) {
        Specification<VehicleMaintenance> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (vehicleId != null) ps.add(cb.equal(root.get("vehicle").get("id"), vehicleId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(maintenanceRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "scheduledDate")),
                MasterDataMapper::toMaintenance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> due() {
        return maintenanceRepository.findByStatusInAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
                        EnumSet.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS), LocalDate.now().plusDays(7))
                .stream().map(MasterDataMapper::toMaintenance).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse get(Long id) {
        return MasterDataMapper.toMaintenance(find(id));
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "MAINTENANCE", entity = "VehicleMaintenance")
    public MaintenanceResponse create(MaintenanceRequest r) {
        Vehicle v = vehicle(r.getVehicleId());
        VehicleMaintenance m = new VehicleMaintenance();
        m.setVehicle(v);
        apply(m, r);
        maintenanceRepository.save(m);
        afterStatus(m, null);
        return MasterDataMapper.toMaintenance(m);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "MAINTENANCE", entity = "VehicleMaintenance")
    public MaintenanceResponse update(Long id, MaintenanceRequest r) {
        VehicleMaintenance m = find(id);
        if (m.getStatus() == MaintenanceStatus.COMPLETED || m.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Completed or cancelled records cannot be edited");
        }
        MaintenanceStatus before = m.getStatus();
        if (!m.getVehicle().getId().equals(r.getVehicleId())) {
            m.setVehicle(vehicle(r.getVehicleId()));
        }
        apply(m, r);
        afterStatus(m, before);
        return MasterDataMapper.toMaintenance(m);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "MAINTENANCE", entity = "VehicleMaintenance")
    public MaintenanceResponse complete(Long id, MaintenanceRequest.Complete r) {
        VehicleMaintenance m = find(id);
        if (m.getStatus() == MaintenanceStatus.COMPLETED || m.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(m.getStatus(), MaintenanceStatus.COMPLETED);
        }
        if (r.getCompletedDate().isBefore(m.getScheduledDate())) {
            throw new BadRequestException("completedDate", "Completed date must be on or after the scheduled date");
        }
        MaintenanceStatus before = m.getStatus();
        m.setCompletedDate(r.getCompletedDate());
        if (r.getTotalCost() != null) m.setTotalCost(r.getTotalCost());
        if (r.getNextServiceDate() != null) m.setNextServiceDate(r.getNextServiceDate());
        if (r.getNextServiceOdometer() != null) m.setNextServiceOdometer(r.getNextServiceOdometer());
        m.setStatus(MaintenanceStatus.COMPLETED);
        afterStatus(m, before);
        return MasterDataMapper.toMaintenance(m);
    }

    // ---------- helpers ----------

    private void apply(VehicleMaintenance m, MaintenanceRequest r) {
        Vehicle v = m.getVehicle();
        if (r.getOdometerKm() < v.getOdometerKm()) {
            throw new BadRequestException("odometerKm", "Odometer must be at least the vehicle reading (" + v.getOdometerKm() + " km)");
        }
        if (r.getStatus() == MaintenanceStatus.COMPLETED) {
            if (r.getCompletedDate() == null) {
                throw new BadRequestException("completedDate", "Completed date is required when status is COMPLETED");
            }
            if (r.getCompletedDate().isBefore(r.getScheduledDate())) {
                throw new BadRequestException("completedDate", "Completed date must be on or after the scheduled date");
            }
        }
        if (r.getNextServiceDate() != null && !r.getNextServiceDate().isAfter(r.getScheduledDate())) {
            throw new BadRequestException("nextServiceDate", "Next service must be after the scheduled date");
        }
        m.setMaintenanceType(r.getMaintenanceType());
        m.setDescription(r.getDescription().trim());
        m.setScheduledDate(r.getScheduledDate());
        m.setCompletedDate(r.getStatus() == MaintenanceStatus.COMPLETED ? r.getCompletedDate() : null);
        m.setOdometerKm(r.getOdometerKm());
        m.setServiceCenter(AuthServiceImpl.blankToNull(r.getServiceCenter()));
        m.setLabourCost(r.getLabourCost());
        m.setNextServiceDate(r.getNextServiceDate());
        m.setNextServiceOdometer(r.getNextServiceOdometer());
        m.setInvoiceUrl(AuthServiceImpl.blankToNull(r.getInvoiceUrl()));
        m.setStatus(r.getStatus());

        m.getParts().clear();
        BigDecimal partsTotal = BigDecimal.ZERO;
        for (MaintenanceRequest.Part pr : r.getParts()) {
            MaintenancePart p = new MaintenancePart();
            p.setMaintenance(m);
            p.setPartName(pr.getPartName().trim());
            p.setPartNumber(AuthServiceImpl.blankToNull(pr.getPartNumber()));
            p.setQuantity(pr.getQuantity());
            p.setUnitCost(pr.getUnitCost());
            p.setTotalCost(pr.getUnitCost().multiply(BigDecimal.valueOf(pr.getQuantity())));
            partsTotal = partsTotal.add(p.getTotalCost());
            m.getParts().add(p);
        }
        BigDecimal computed = partsTotal.add(r.getLabourCost() == null ? BigDecimal.ZERO : r.getLabourCost());
        m.setTotalCost(r.getTotalCost() != null ? r.getTotalCost() : computed);
    }

    /** Keeps the vehicle status, odometer and service dates in step with the maintenance record. */
    private void afterStatus(VehicleMaintenance m, MaintenanceStatus before) {
        Vehicle v = m.getVehicle();
        if (m.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            if (v.getStatus() == VehicleStatus.ON_TRIP) {
                throw new BadRequestException("status", v.getVehicleNumber() + " is on a trip; start maintenance after it returns");
            }
            v.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        } else if (m.getStatus() == MaintenanceStatus.COMPLETED) {
            if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                v.setStatus(VehicleStatus.AVAILABLE);
            }
            if (m.getOdometerKm() != null && m.getOdometerKm() > v.getOdometerKm()) {
                v.setOdometerKm(m.getOdometerKm());
            }
            v.setLastServiceDate(m.getCompletedDate());
            v.setNextServiceDate(m.getNextServiceDate());
        } else if (before == MaintenanceStatus.IN_PROGRESS && v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
            v.setStatus(VehicleStatus.AVAILABLE);
        }
        if (m.getStatus() == MaintenanceStatus.SCHEDULED && (v.getNextServiceDate() == null
                || m.getScheduledDate().isBefore(v.getNextServiceDate()))) {
            v.setNextServiceDate(m.getScheduledDate());
        }
        statusManager.pushDashboard();
    }

    private Vehicle vehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    private VehicleMaintenance find(Long id) {
        return maintenanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Maintenance", id));
    }
}
