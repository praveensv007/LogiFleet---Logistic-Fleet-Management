package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.VehicleResponse;
import com.logistics.entity.Vehicle;
import com.logistics.entity.VehicleDocument;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.FleetMapper;
import com.logistics.mapper.MasterDataMapper;
import com.logistics.repository.*;
import com.logistics.service.VehicleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final WarehouseRepository warehouseRepository;
    private final VehicleMaintenanceRepository maintenanceRepository;
    private final FleetAssignmentRepository assignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> list(String search, VehicleStatus status, VehicleType type, int page, int size, String sort) {
        Specification<Vehicle> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                ps.add(cb.or(cb.like(cb.lower(root.get("vehicleNumber")), like),
                        cb.like(cb.lower(root.get("make")), like),
                        cb.like(cb.lower(root.get("model")), like)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (type != null) ps.add(cb.equal(root.get("vehicleType"), type));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(vehicleRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "createdAt")),
                MasterDataMapper::toVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> available(BigDecimal minKg) {
        return vehicleRepository.findByStatusAndCapacityKgGreaterThanEqualAndInsuranceExpiryAfterOrderByCapacityKgAsc(
                        VehicleStatus.AVAILABLE, minKg == null ? BigDecimal.ZERO : minKg, LocalDate.now())
                .stream().map(MasterDataMapper::toVehicle).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse get(Long id) {
        Vehicle v = find(id);
        VehicleResponse res = MasterDataMapper.toVehicle(v);
        res.setDocuments(documents(id));
        res.setMaintenanceHistory(maintenanceRepository.findByVehicle_IdOrderByScheduledDateDesc(id).stream()
                .map(MasterDataMapper::toMaintenance).toList());
        res.setRecentTrips(assignmentRepository.findByVehicle_IdOrderByAssignedAtDesc(id, PageRequest.of(0, 10))
                .stream().map(FleetMapper::toAssignment).toList());
        return res;
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "VEHICLES", entity = "Vehicle")
    public VehicleResponse create(VehicleRequest r) {
        if (r.getStatus() == VehicleStatus.ON_TRIP) {
            throw new BadRequestException("status", "ON_TRIP is set automatically by an assignment");
        }
        String number = normalise(r.getVehicleNumber());
        if (vehicleRepository.existsByVehicleNumber(number))
            throw new BadRequestException("vehicleNumber", "Vehicle number is already registered");
        if (vehicleRepository.existsByChassisNumber(r.getChassisNumber().trim()))
            throw new BadRequestException("chassisNumber", "Chassis number is already registered");
        if (vehicleRepository.existsByEngineNumber(r.getEngineNumber().trim()))
            throw new BadRequestException("engineNumber", "Engine number is already registered");
        validate(r);
        Vehicle v = new Vehicle();
        apply(v, r);
        return MasterDataMapper.toVehicle(vehicleRepository.save(v));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "VEHICLES", entity = "Vehicle")
    public VehicleResponse update(Long id, VehicleRequest r) {
        Vehicle v = find(id);
        String number = normalise(r.getVehicleNumber());
        if (vehicleRepository.existsByVehicleNumberAndIdNot(number, id))
            throw new BadRequestException("vehicleNumber", "Vehicle number is already registered");
        if (vehicleRepository.existsByChassisNumberAndIdNot(r.getChassisNumber().trim(), id))
            throw new BadRequestException("chassisNumber", "Chassis number is already registered");
        if (vehicleRepository.existsByEngineNumberAndIdNot(r.getEngineNumber().trim(), id))
            throw new BadRequestException("engineNumber", "Engine number is already registered");
        if (r.getOdometerKm() < v.getOdometerKm()) {
            throw new BadRequestException("odometerKm", "Odometer cannot go below " + v.getOdometerKm() + " km");
        }
        if (r.getStatus() != v.getStatus()) {
            checkStatusChange(v, r.getStatus());
        }
        validate(r);
        apply(v, r);
        return MasterDataMapper.toVehicle(v);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "VEHICLES", entity = "Vehicle")
    public VehicleResponse changeStatus(Long id, VehicleStatus status) {
        Vehicle v = find(id);
        checkStatusChange(v, status);
        v.setStatus(status);
        return MasterDataMapper.toVehicle(v);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse.VehicleDocumentResponse> documents(Long vehicleId) {
        return documentRepository.findByVehicle_IdOrderByIdDesc(vehicleId).stream()
                .map(MasterDataMapper::toDocument).toList();
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "VEHICLES", entity = "VehicleDocument")
    public VehicleResponse.VehicleDocumentResponse addDocument(Long vehicleId, MasterDataRequests.VehicleDocument r) {
        Vehicle v = find(vehicleId);
        VehicleDocument d = new VehicleDocument();
        d.setVehicle(v);
        d.setDocumentType(r.getDocumentType());
        d.setDocumentNumber(r.getDocumentNumber());
        d.setFileUrl(r.getFileUrl());
        d.setExpiryDate(r.getExpiryDate());
        documentRepository.save(d);
        // keep the vehicle's own expiry columns in sync with the newest document
        if (r.getExpiryDate() != null) {
            switch (r.getDocumentType()) {
                case INSURANCE -> v.setInsuranceExpiry(r.getExpiryDate());
                case PERMIT -> v.setPermitExpiry(r.getExpiryDate());
                case FITNESS -> v.setFitnessExpiry(r.getExpiryDate());
                case PUC -> v.setPucExpiry(r.getExpiryDate());
                default -> { }
            }
        }
        return MasterDataMapper.toDocument(d);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, module = "VEHICLES", entity = "VehicleDocument")
    public void deleteDocument(Long vehicleId, Long documentId) {
        VehicleDocument d = documentRepository.findById(documentId)
                .filter(doc -> doc.getVehicle().getId().equals(vehicleId))
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        documentRepository.delete(d);
    }

    private void checkStatusChange(Vehicle v, VehicleStatus to) {
        if (to == VehicleStatus.ON_TRIP) {
            throw new BadRequestException("status", "ON_TRIP is set automatically by an assignment");
        }
        boolean activeTrip = !assignmentRepository.findByStatusIn(
                        EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))
                .stream().filter(a -> a.getVehicle().getId().equals(v.getId())).toList().isEmpty();
        if (activeTrip) {
            throw new BadRequestException("status", "Vehicle is on an active trip; finish or cancel it first");
        }
    }

    private void validate(VehicleRequest r) {
        if (r.getManufactureYear() > Year.now().getValue()) {
            throw new BadRequestException("manufactureYear", "Year cannot be in the future");
        }
    }

    private void apply(Vehicle v, VehicleRequest r) {
        v.setVehicleNumber(normalise(r.getVehicleNumber()));
        v.setVehicleType(r.getVehicleType());
        v.setMake(r.getMake().trim());
        v.setModel(r.getModel().trim());
        v.setManufactureYear(r.getManufactureYear());
        v.setFuelType(r.getFuelType());
        v.setCapacityKg(r.getCapacityKg());
        v.setCapacityCbm(r.getCapacityCbm());
        v.setChassisNumber(r.getChassisNumber().trim());
        v.setEngineNumber(r.getEngineNumber().trim());
        v.setInsuranceNumber(r.getInsuranceNumber().trim());
        v.setInsuranceExpiry(r.getInsuranceExpiry());
        v.setPermitExpiry(r.getPermitExpiry());
        v.setFitnessExpiry(r.getFitnessExpiry());
        v.setPucExpiry(r.getPucExpiry());
        v.setOdometerKm(r.getOdometerKm());
        v.setHomeWarehouse(r.getHomeWarehouseId() == null ? null : warehouseRepository.findById(r.getHomeWarehouseId())
                .filter(w -> w.getStatus() == ActiveStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("homeWarehouseId", "Choose an active warehouse")));
        v.setStatus(r.getStatus());
    }

    private static String normalise(String number) {
        return number.replaceAll("[\\s-]", "").toUpperCase();
    }

    private Vehicle find(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }
}
