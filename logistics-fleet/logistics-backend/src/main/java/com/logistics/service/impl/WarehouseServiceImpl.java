package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.request.WarehouseOpsRequests;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.dto.response.WarehouseResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.MasterDataMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.*;
import com.logistics.security.SecurityUtils;
import com.logistics.security.UserPrincipal;
import com.logistics.service.NotificationService;
import com.logistics.service.WarehouseService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseTransactionRepository txnRepository;
    private final ShipmentRepository shipmentRepository;
    private final PackageRepository packageRepository;
    private final FleetAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ShipmentStatusManager statusManager;
    private final NotificationService notificationService;
    private final WebSocketPublisher publisher;

    // ---------------- master data ----------------

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> list(ActiveStatus status) {
        List<Warehouse> list = status == null ? warehouseRepository.findAll(Sort.by("name"))
                : warehouseRepository.findByStatusOrderByNameAsc(status);
        return list.stream().map(w -> MasterDataMapper.toWarehouse(w,
                shipmentRepository.countByOriginWarehouse_IdAndCurrentStatusIn(w.getId(), ShipmentStatus.WAREHOUSE_QUEUE))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse get(Long id) {
        Warehouse w = findWarehouse(id);
        return MasterDataMapper.toWarehouse(w,
                shipmentRepository.countByOriginWarehouse_IdAndCurrentStatusIn(id, ShipmentStatus.WAREHOUSE_QUEUE));
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "WAREHOUSES", entity = "Warehouse")
    public WarehouseResponse create(MasterDataRequests.Warehouse r) {
        if (warehouseRepository.existsByWarehouseCode(r.getWarehouseCode().trim().toUpperCase())) {
            throw new BadRequestException("warehouseCode", "Warehouse code is already used");
        }
        Warehouse w = new Warehouse();
        apply(w, r);
        return MasterDataMapper.toWarehouse(warehouseRepository.save(w), 0);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "WAREHOUSES", entity = "Warehouse")
    public WarehouseResponse update(Long id, MasterDataRequests.Warehouse r) {
        Warehouse w = findWarehouse(id);
        if (warehouseRepository.existsByWarehouseCodeAndIdNot(r.getWarehouseCode().trim().toUpperCase(), id)) {
            throw new BadRequestException("warehouseCode", "Warehouse code is already used");
        }
        apply(w, r);
        return get(id);
    }

    private void apply(Warehouse w, MasterDataRequests.Warehouse r) {
        if (!r.getClosingTime().isAfter(r.getOpeningTime())) {
            throw new BadRequestException("closingTime", "Closing time must be after opening time");
        }
        User manager = userRepository.findById(r.getManagerUserId())
                .orElseThrow(() -> new BadRequestException("managerUserId", "Manager not found"));
        String role = manager.getPrimaryRole();
        if (!RoleName.WAREHOUSE_STAFF.name().equals(role) && !RoleName.ADMIN.name().equals(role)) {
            throw new BadRequestException("managerUserId", "Manager must be warehouse staff or an admin");
        }
        w.setWarehouseCode(r.getWarehouseCode().trim().toUpperCase());
        w.setName(r.getName().trim());
        w.setAddressLine1(r.getAddressLine1());
        w.setCity(r.getCity());
        w.setState(r.getState());
        w.setPincode(r.getPincode());
        w.setCountry(r.getCountry());
        w.setLatitude(r.getLatitude());
        w.setLongitude(r.getLongitude());
        w.setManager(manager);
        w.setContactPhone(r.getContactPhone());
        w.setContactEmail(r.getContactEmail());
        w.setCapacity(r.getCapacity());
        w.setOpeningTime(r.getOpeningTime());
        w.setClosingTime(r.getClosingTime());
        w.setStatus(r.getStatus());
    }

    // ---------------- operations ----------------

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "WAREHOUSE", entity = "Shipment")
    public ShipmentResponse receive(WarehouseOpsRequests.Receive r) {
        Shipment s = shipmentRepository.findByTrackingNumber(r.getTrackingNumber().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("trackingNumber", "No shipment with this tracking number"));
        checkAccess(s);
        if (s.getCurrentStatus() != ShipmentStatus.CREATED) {
            throw new InvalidStatusTransitionException("Shipment is already " + s.getCurrentStatus() + "; only CREATED shipments can be received");
        }
        String base = join(r.getRemarks(), r.getPackageCount().equals(s.getTotalPackages()) ? null
                : "Count mismatch: expected " + s.getTotalPackages() + ", received " + r.getPackageCount());
        final String remarks = r.getConditionStatus() == ConditionStatus.DAMAGED
                ? join("Received with damage", base) : base;
        saveTxn(s.getOriginWarehouse(), s, null, null, WarehouseTxnType.RECEIVED, r.getPackageCount(),
                r.getConditionStatus(), null, null, remarks);
        s.getPackages().forEach(p -> statusManager.changePackageStatus(p,
                r.getConditionStatus() == ConditionStatus.DAMAGED ? PackageStatus.DAMAGED : PackageStatus.RECEIVED,
                s.getOriginWarehouse(), remarks));
        statusManager.changeStatus(s, ShipmentStatus.RECEIVED, location(s), remarks != null ? remarks : "Received at warehouse");
        return detail(s);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "WAREHOUSE", entity = "Package")
    public ShipmentResponse pack(WarehouseOpsRequests.Pack r) {
        ShipmentPackage p = packageRepository.findById(r.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package", r.getPackageId()));
        Shipment s = p.getShipment();
        checkAccess(s);
        if (s.getCurrentStatus() != ShipmentStatus.RECEIVED && s.getCurrentStatus() != ShipmentStatus.PACKED) {
            throw new InvalidStatusTransitionException("Receive the shipment before packing");
        }
        if (p.getCurrentStatus() != PackageStatus.RECEIVED && p.getCurrentStatus() != PackageStatus.DAMAGED
                && p.getCurrentStatus() != PackageStatus.PACKED) {
            throw new InvalidStatusTransitionException("Package " + p.getPackageCode() + " is " + p.getCurrentStatus());
        }
        p.setPackingType(r.getPackingType());
        String remarks = join(r.isLabelPrinted() ? "Label printed" : "Label not printed", r.getRemarks());
        statusManager.changePackageStatus(p, PackageStatus.PACKED, s.getOriginWarehouse(), remarks);
        saveTxn(s.getOriginWarehouse(), s, p, null, WarehouseTxnType.PACKED, p.getQuantity(), null, null, null,
                "Packed in " + r.getPackingType() + ". " + remarks);
        boolean allPacked = s.getPackages().stream().allMatch(x -> x.getCurrentStatus() == PackageStatus.PACKED);
        if (allPacked && s.getCurrentStatus() == ShipmentStatus.RECEIVED) {
            statusManager.changeStatus(s, ShipmentStatus.PACKED, location(s), "All packages packed");
        }
        return detail(s);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "WAREHOUSE", entity = "Shipment")
    public ShipmentResponse markReady(WarehouseOpsRequests.Ready r) {
        Shipment s = findShipment(r.getShipmentId());
        checkAccess(s);
        if (s.getCurrentStatus() != ShipmentStatus.PACKED) {
            throw new InvalidStatusTransitionException("Pack every package before marking the shipment ready");
        }
        saveTxn(s.getOriginWarehouse(), s, null, null, WarehouseTxnType.PACKED, s.getTotalPackages(), null,
                r.getStorageLocation().trim().toUpperCase(), null, join("Ready for dispatch", r.getRemarks()));
        statusManager.changeStatus(s, ShipmentStatus.READY_FOR_DISPATCH, location(s),
                "Stored at " + r.getStorageLocation().trim().toUpperCase() + ", waiting for a vehicle");
        notificationService.notifyRole(RoleName.FLEET_MANAGER, "Shipment ready for assignment",
                s.getTrackingNumber() + " (" + s.getTotalWeightKg() + " kg) at " + s.getOriginWarehouse().getName()
                        + " needs a driver and vehicle.", NotificationType.PENDING_SHIPMENT, ReferenceType.SHIPMENT, s.getId());
        return detail(s);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "WAREHOUSE", entity = "Assignment")
    public ShipmentResponse loadOrUnload(WarehouseOpsRequests.Loading r) {
        if (r.getTransactionType() != WarehouseTxnType.LOADING && r.getTransactionType() != WarehouseTxnType.UNLOADING) {
            throw new BadRequestException("transactionType", "Choose LOADING or UNLOADING");
        }
        FleetAssignment a = assignmentRepository.findById(r.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", r.getAssignmentId()));
        Shipment s = a.getShipment();
        checkAccess(s);
        boolean loading = r.getTransactionType() == WarehouseTxnType.LOADING;
        if (loading) {
            if (a.getStatus() != AssignmentStatus.ACCEPTED) {
                throw new InvalidStatusTransitionException("The driver must accept the trip before loading");
            }
            if (!r.getPackageCount().equals(s.getTotalPackages())) {
                throw new BadRequestException("packageCount", "Load all " + s.getTotalPackages() + " packages");
            }
            s.getPackages().forEach(p -> statusManager.changePackageStatus(p, PackageStatus.LOADED,
                    s.getOriginWarehouse(), "Loaded onto " + a.getVehicle().getVehicleNumber()));
        } else {
            if (s.getCurrentStatus() != ShipmentStatus.ASSIGNED) {
                throw new InvalidStatusTransitionException("Only shipments not yet dispatched can be unloaded here");
            }
            s.getPackages().forEach(p -> statusManager.changePackageStatus(p, PackageStatus.PACKED,
                    s.getOriginWarehouse(), "Unloaded from " + a.getVehicle().getVehicleNumber()));
        }
        saveTxn(s.getOriginWarehouse(), s, null, a, r.getTransactionType(), r.getPackageCount(), null, null, null,
                join((loading ? "Loaded onto " : "Unloaded from ") + a.getVehicle().getVehicleNumber(), r.getRemarks()));
        return detail(s);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "WAREHOUSE", entity = "Shipment")
    public ShipmentResponse dispatch(WarehouseOpsRequests.Dispatch r) {
        Shipment s = findShipment(r.getShipmentId());
        checkAccess(s);
        if (s.getCurrentStatus() != ShipmentStatus.ASSIGNED) {
            throw new InvalidStatusTransitionException("Only ASSIGNED shipments can be dispatched");
        }
        FleetAssignment a = assignmentRepository.findFirstByShipment_IdAndStatusInOrderByIdDesc(s.getId(),
                        EnumSet.of(AssignmentStatus.ACCEPTED))
                .orElseThrow(() -> new InvalidStatusTransitionException("The driver has not accepted the trip yet"));
        if (s.getPackages().stream().anyMatch(p -> p.getCurrentStatus() != PackageStatus.LOADED)) {
            throw new InvalidStatusTransitionException("Confirm loading of every package before dispatch");
        }
        LocalDateTime time = r.getDispatchTime() != null ? r.getDispatchTime() : LocalDateTime.now();
        WarehouseTransaction t = saveTxn(s.getOriginWarehouse(), s, null, a, WarehouseTxnType.DISPATCHED,
                s.getTotalPackages(), null, null, r.getGatePassNumber().trim(), r.getRemarks());
        t.setTransactionTime(time);
        statusManager.changeStatus(s, ShipmentStatus.DISPATCHED, location(s),
                "Dispatched on " + a.getVehicle().getVehicleNumber() + ", gate pass " + r.getGatePassNumber().trim());
        if (a.getDriver().getUser() != null) {
            notificationService.notifyUser(a.getDriver().getUser(), "Shipment dispatched",
                    s.getTrackingNumber() + " is loaded and dispatched. Mark it PICKED_UP when you leave.",
                    NotificationType.SHIPMENT_STATUS_CHANGED, ReferenceType.ASSIGNMENT, a.getId());
        }
        return detail(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> queue(Long warehouseId) {
        UserPrincipal p = SecurityUtils.current();
        if (p.hasRole(RoleName.WAREHOUSE_STAFF.name()) && !warehouseId.equals(p.getWarehouseId())) {
            throw new ForbiddenException("You can only view your own warehouse queue");
        }
        return shipmentRepository.findByOriginWarehouse_IdAndCurrentStatusIn(warehouseId, ShipmentStatus.WAREHOUSE_QUEUE,
                        Sort.by(Sort.Direction.ASC, "createdAt"))
                .stream().map(this::detail).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse.WarehouseTxnResponse> transactions(Long shipmentId) {
        return txnRepository.findByShipment_IdOrderByTransactionTimeAsc(shipmentId).stream()
                .map(ShipmentMapper::toTxn).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse findByTracking(String trackingNumber) {
        Shipment s = shipmentRepository.findByTrackingNumber(trackingNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", trackingNumber));
        checkAccess(s);
        return detail(s);
    }

    // ---------------- helpers ----------------

    private ShipmentResponse detail(Shipment s) {
        FleetAssignment a = assignmentRepository.findFirstByShipment_IdOrderByIdDesc(s.getId()).orElse(null);
        ShipmentResponse res = ShipmentMapper.toResponse(s, true, a);
        res.setStorageLocation(txnRepository.findByShipment_IdOrderByTransactionTimeAsc(s.getId()).stream()
                .map(WarehouseTransaction::getStorageLocation).filter(x -> x != null).reduce((x, y) -> y).orElse(null));
        return res;
    }

    private WarehouseTransaction saveTxn(Warehouse w, Shipment s, ShipmentPackage p, FleetAssignment a,
                                         WarehouseTxnType type, Integer count, ConditionStatus condition,
                                         String storage, String gatePass, String remarks) {
        WarehouseTransaction t = new WarehouseTransaction();
        t.setWarehouse(w);
        t.setShipment(s);
        t.setShipmentPackage(p);
        t.setAssignment(a);
        t.setTransactionType(type);
        t.setPackageCount(count);
        t.setConditionStatus(condition);
        t.setStorageLocation(storage);
        t.setGatePassNumber(gatePass);
        t.setPerformedBy(userRepository.getReferenceById(SecurityUtils.current().getId()));
        t.setTransactionTime(LocalDateTime.now());
        t.setRemarks(remarks != null && remarks.length() > 500 ? remarks.substring(0, 500) : remarks);
        txnRepository.save(t);
        publisher.warehouse(w != null ? w.getId() : null, StatusUpdateMessage.builder()
                .event("WAREHOUSE_TXN").shipmentId(s.getId()).trackingNumber(s.getTrackingNumber())
                .status(type.name()).build());
        return t;
    }

    /** Warehouse staff may only work on shipments of their own warehouse. */
    private void checkAccess(Shipment s) {
        UserPrincipal p = SecurityUtils.current();
        if (p.hasRole(RoleName.WAREHOUSE_STAFF.name())) {
            Long own = p.getWarehouseId();
            if (own == null || s.getOriginWarehouse() == null || !own.equals(s.getOriginWarehouse().getId())) {
                throw new ForbiddenException("Shipment " + s.getTrackingNumber() + " belongs to another warehouse");
            }
        }
    }

    private Shipment findShipment(Long id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Warehouse", id));
    }

    private static String location(Shipment s) {
        return Optional.ofNullable(s.getOriginWarehouse()).map(w -> w.getName() + ", " + w.getCity()).orElse(null);
    }

    private static String join(String a, String b) {
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + ". " + b;
    }
}
