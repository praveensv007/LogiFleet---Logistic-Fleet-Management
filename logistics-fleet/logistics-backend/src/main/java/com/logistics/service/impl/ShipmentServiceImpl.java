package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.AddressRequest;
import com.logistics.dto.request.PackageRequest;
import com.logistics.dto.request.ShipmentCreateRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.dto.response.TrackingResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.*;
import com.logistics.security.SecurityUtils;
import com.logistics.security.UserPrincipal;
import com.logistics.service.NotificationService;
import com.logistics.service.ShipmentService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    static final EnumSet<AssignmentStatus> OPEN_ASSIGNMENT =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS);

    private final ShipmentRepository shipmentRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShipmentStatusHistoryRepository historyRepository;
    private final FleetAssignmentRepository assignmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final TrackingNumberGenerator generator;
    private final ShipmentStatusManager statusManager;
    private final NotificationService notificationService;
    private final WebSocketPublisher publisher;

    @Override
    @Auditable(action = AuditAction.CREATE, module = "SHIPMENTS", entity = "Shipment")
    public ShipmentResponse create(ShipmentCreateRequest r) {
        Customer customer = resolveCustomer(r.getCustomerId());
        if (customer.getStatus() != ActiveStatus.ACTIVE) {
            throw new BadRequestException("Customer account is inactive");
        }
        Shipment s = new Shipment();
        s.setTrackingNumber(generator.nextTrackingNumber());
        s.setCustomer(customer);
        s.setOriginWarehouse(resolveOrigin(r.getOriginWarehouseId(), r.getPickup()));
        s.setServiceType(r.getServiceType());
        s.setDescription(r.getDescription().trim());
        s.setDeclaredValue(r.getDeclaredValue());
        s.setPaymentMode(r.getPaymentMode());
        s.setSpecialInstructions(AuthServiceImpl.blankToNull(r.getSpecialInstructions()));
        s.setPickupDate(r.getPickupDate());
        s.setPickupTimeSlot(r.getPickupTimeSlot());
        s.addAddress(toAddress(r.getPickup(), AddressType.PICKUP));
        s.addAddress(toAddress(r.getDelivery(), AddressType.DELIVERY));
        int i = 1;
        for (PackageRequest pr : r.getPackages()) {
            ShipmentPackage p = new ShipmentPackage();
            applyPackage(p, pr);
            p.setPackageCode(s.getTrackingNumber() + "-P" + i++);
            s.addPackage(p);
        }
        recalculate(s);
        s.setCurrentStatus(ShipmentStatus.CREATED);
        shipmentRepository.save(s);
        statusManager.recordCreated(s);

        Warehouse origin = s.getOriginWarehouse();
        notificationService.notifyWarehouse(origin.getId(), "New shipment " + s.getTrackingNumber(),
                s.getTotalPackages() + " package(s), " + s.getTotalWeightKg() + " kg from "
                        + r.getPickup().getCity() + ". Pickup " + s.getPickupDate() + " " + s.getPickupTimeSlot(),
                NotificationType.SHIPMENT_CREATED, ReferenceType.SHIPMENT, s.getId());
        if (customer.getUser() != null) {
            notificationService.notifyUser(customer.getUser(), "Shipment booked",
                    "Tracking number " + s.getTrackingNumber() + ". Estimated delivery " + s.getEstimatedDeliveryDate() + ".",
                    NotificationType.SHIPMENT_CREATED, ReferenceType.SHIPMENT, s.getId());
        }
        publisher.warehouse(origin.getId(), StatusUpdateMessage.builder().event("NEW_SHIPMENT")
                .shipmentId(s.getId()).trackingNumber(s.getTrackingNumber()).status(ShipmentStatus.CREATED.name()).build());
        return ShipmentMapper.toResponse(s, true, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> list(ShipmentStatus status, LocalDate from, LocalDate to, Long warehouseId,
                                               String search, int page, int size, String sort) {
        UserPrincipal p = SecurityUtils.current();
        Long ownCustomerId = null;
        if (!p.hasAuthority("SHIPMENT_VIEW_ALL")) {
            ownCustomerId = customerRepository.findByUser_Id(p.getId()).map(Customer::getId)
                    .orElseThrow(() -> new ForbiddenException("You cannot list shipments"));
        }
        final Long customerId = ownCustomerId;
        Specification<Shipment> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (customerId != null) ps.add(cb.equal(root.get("customer").get("id"), customerId));
            if (status != null) ps.add(cb.equal(root.get("currentStatus"), status));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) ps.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            if (warehouseId != null) ps.add(cb.equal(root.get("originWarehouse").get("id"), warehouseId));
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                Join<Shipment, Customer> c = root.join("customer");
                ps.add(cb.or(cb.like(cb.lower(root.get("trackingNumber")), like),
                        cb.like(cb.lower(c.get("fullName")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(shipmentRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "createdAt")),
                s -> ShipmentMapper.toResponse(s, false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse get(Long id) {
        Shipment s = findAllowed(id);
        return ShipmentMapper.toResponse(s, true,
                assignmentRepository.findFirstByShipment_IdOrderByIdDesc(id).orElse(null));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "SHIPMENTS", entity = "Shipment")
    public ShipmentResponse update(Long id, ShipmentUpdateRequest r) {
        Shipment s = findAllowed(id);
        checkCanEdit(s);
        if (s.getCurrentStatus() != ShipmentStatus.CREATED) {
            throw new InvalidStatusTransitionException("Shipments can only be edited before the warehouse receives them");
        }
        copyAddress(r.getPickup(), s.getAddress(AddressType.PICKUP));
        copyAddress(r.getDelivery(), s.getAddress(AddressType.DELIVERY));
        s.setPickupDate(r.getPickupDate());
        s.setPickupTimeSlot(r.getPickupTimeSlot());
        s.setServiceType(r.getServiceType());
        s.setDescription(r.getDescription().trim());
        s.setDeclaredValue(r.getDeclaredValue());
        s.setPaymentMode(r.getPaymentMode());
        s.setSpecialInstructions(AuthServiceImpl.blankToNull(r.getSpecialInstructions()));
        recalculate(s);
        return ShipmentMapper.toResponse(s, true, null);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "SHIPMENTS", entity = "Shipment")
    public ShipmentResponse cancel(Long id, String reason) {
        Shipment s = findAllowed(id);
        checkCanEdit(s);
        if (!s.getCurrentStatus().isCancellable()) {
            throw new InvalidStatusTransitionException("Shipments can only be cancelled before PICKED_UP");
        }
        assignmentRepository.findFirstByShipment_IdAndStatusInOrderByIdDesc(id, OPEN_ASSIGNMENT).ifPresent(a -> {
            a.setStatus(AssignmentStatus.CANCELLED);
            a.getDriver().setAvailabilityStatus(DriverAvailability.AVAILABLE);
            a.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            if (a.getDriver().getUser() != null) {
                notificationService.notifyUser(a.getDriver().getUser(), "Trip cancelled",
                        s.getTrackingNumber() + " was cancelled: " + reason, NotificationType.SHIPMENT_STATUS_CHANGED,
                        ReferenceType.ASSIGNMENT, a.getId());
            }
        });
        s.setCancelReason(reason);
        statusManager.changeStatus(s, ShipmentStatus.CANCELLED, null, "Cancelled: " + reason);
        return ShipmentMapper.toResponse(s, true, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse.StatusHistoryResponse> history(Long id) {
        findAllowed(id);
        return historyRepository.findByShipment_IdOrderByChangedAtAscIdAsc(id).stream()
                .map(ShipmentMapper::toHistory).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse track(String trackingNumber) {
        Shipment s = shipmentRepository.findByTrackingNumber(trackingNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No shipment found for " + trackingNumber));
        ShipmentAddress from = s.getAddress(AddressType.PICKUP);
        ShipmentAddress to = s.getAddress(AddressType.DELIVERY);
        TrackingResponse.TrackingResponseBuilder b = TrackingResponse.builder()
                .trackingNumber(s.getTrackingNumber())
                .currentStatus(s.getCurrentStatus().name())
                .serviceType(s.getServiceType().name())
                .originCity(from != null ? from.getCity() : null)
                .originState(from != null ? from.getState() : null)
                .destinationCity(to != null ? to.getCity() : null)
                .destinationState(to != null ? to.getState() : null)
                .receiverName(to != null ? to.getContactName() : null)
                .originWarehouseName(s.getOriginWarehouse() != null ? s.getOriginWarehouse().getName() : null)
                .totalPackages(s.getTotalPackages())
                .totalWeightKg(s.getTotalWeightKg())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .bookedAt(s.getCreatedAt())
                .timeline(historyRepository.findByShipment_IdOrderByChangedAtAscIdAsc(s.getId()).stream()
                        .map(ShipmentMapper::toHistory).toList());

        boolean afterAssigned = s.getCurrentStatus() != ShipmentStatus.CANCELLED
                && s.getCurrentStatus().ordinal() >= ShipmentStatus.DISPATCHED.ordinal()
                || s.getCurrentStatus() == ShipmentStatus.ASSIGNED;
        if (afterAssigned) {
            assignmentRepository.findFirstByShipment_IdAndStatusInOrderByIdDesc(s.getId(),
                    EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS,
                            AssignmentStatus.COMPLETED)).ifPresent(a -> b
                    .driverName(a.getDriver().getFullName())
                    .driverPhone(a.getDriver().getPhone())
                    .vehicleNumber(a.getVehicle().getVehicleNumber()));
        }
        deliveryRepository.findFirstByShipment_IdOrderByIdDesc(s.getId()).ifPresent(d -> {
            b.deliveryId(d.getId())
                    .currentLatitude(d.getCurrentLatitude())
                    .currentLongitude(d.getCurrentLongitude())
                    .lastLocationUpdate(d.getLastLocationUpdate())
                    .delayed(d.isDelayed());
            if (s.getCurrentStatus() == ShipmentStatus.DELIVERED) {
                b.podReceiverName(d.getReceiverName())
                        .deliveredTime(d.getDeliveredTime())
                        .signatureUrl(d.getSignatureUrl())
                        .podPhotoUrl(d.getPodPhotoUrl());
            }
        });
        return b.build();
    }

    // ---------- helpers shared with PackageServiceImpl ----------

    /** Owner customer, staff with SHIPMENT_VIEW_ALL, or the driver assigned to it. */
    public Shipment findAllowed(Long id) {
        Shipment s = shipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        UserPrincipal p = SecurityUtils.current();
        if (p.hasAuthority("SHIPMENT_VIEW_ALL") || isOwner(s, p)) {
            return s;
        }
        boolean assignedDriver = assignmentRepository.findFirstByShipment_IdOrderByIdDesc(id)
                .map(a -> a.getDriver().getUser() != null && a.getDriver().getUser().getId().equals(p.getId()))
                .orElse(false);
        if (assignedDriver) {
            return s;
        }
        throw new ForbiddenException("You do not have access to this shipment");
    }

    public void checkCanEdit(Shipment s) {
        UserPrincipal p = SecurityUtils.current();
        if (!p.hasAuthority("SHIPMENT_EDIT") && !isOwner(s, p)) {
            throw new ForbiddenException("Only the customer who booked it can change this shipment");
        }
    }

    static boolean isOwner(Shipment s, UserPrincipal p) {
        return s.getCustomer() != null && s.getCustomer().getUser() != null
                && s.getCustomer().getUser().getId().equals(p.getId());
    }

    static void applyPackage(ShipmentPackage p, PackageRequest r) {
        p.setDescription(r.getDescription().trim());
        p.setPackageType(r.getPackageType());
        p.setQuantity(r.getQuantity());
        p.setWeightKg(r.getWeightKg());
        p.setLengthCm(r.getLengthCm());
        p.setWidthCm(r.getWidthCm());
        p.setHeightCm(r.getHeightCm());
        p.setFragile(r.isFragile() || r.getPackageType() == PackageType.FRAGILE);
        p.setVolumetricWeightKg(r.getLengthCm().multiply(r.getWidthCm()).multiply(r.getHeightCm())
                .divide(BigDecimal.valueOf(5000), 2, RoundingMode.HALF_UP));
    }

    /** Totals, shipping charge and estimated delivery date from the package rows. */
    static void recalculate(Shipment s) {
        int count = 0;
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal chargeable = BigDecimal.ZERO;
        for (ShipmentPackage p : s.getPackages()) {
            BigDecimal qty = BigDecimal.valueOf(p.getQuantity());
            count += p.getQuantity();
            weight = weight.add(p.getWeightKg().multiply(qty));
            chargeable = chargeable.add(p.getWeightKg().max(p.getVolumetricWeightKg()).multiply(qty));
        }
        s.setTotalPackages(count);
        s.setTotalWeightKg(weight.setScale(2, RoundingMode.HALF_UP));
        BigDecimal rate = switch (s.getServiceType()) {
            case STANDARD -> BigDecimal.valueOf(50);
            case EXPRESS -> BigDecimal.valueOf(80);
            case SAME_DAY -> BigDecimal.valueOf(120);
        };
        BigDecimal charge = chargeable.multiply(rate).max(BigDecimal.valueOf(100));
        s.setShippingCharge(charge.setScale(2, RoundingMode.HALF_UP));
        int days = switch (s.getServiceType()) {
            case STANDARD -> 5;
            case EXPRESS -> 2;
            case SAME_DAY -> 0;
        };
        s.setEstimatedDeliveryDate(s.getPickupDate().plusDays(days));
    }

    private Customer resolveCustomer(Long customerId) {
        UserPrincipal p = SecurityUtils.current();
        return customerRepository.findByUser_Id(p.getId()).orElseGet(() -> {
            if (customerId == null) {
                throw new BadRequestException("customerId", "Choose the customer this shipment is for");
            }
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        });
    }

    /** The chosen warehouse, else the nearest active one by pincode, city, then state. */
    private Warehouse resolveOrigin(Long warehouseId, AddressRequest pickup) {
        if (warehouseId != null) {
            Warehouse w = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", warehouseId));
            if (w.getStatus() != ActiveStatus.ACTIVE) {
                throw new BadRequestException("originWarehouseId", "Choose an active warehouse");
            }
            return w;
        }
        return warehouseRepository.findFirstByStatusAndPincode(ActiveStatus.ACTIVE, pickup.getPincode())
                .or(() -> warehouseRepository.findFirstByStatusAndCityIgnoreCase(ActiveStatus.ACTIVE, pickup.getCity().trim()))
                .or(() -> warehouseRepository.findFirstByStatusAndStateIgnoreCase(ActiveStatus.ACTIVE, pickup.getState().trim()))
                .or(() -> warehouseRepository.findFirstByStatusOrderByIdAsc(ActiveStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException("originWarehouseId", "No active warehouse is available"));
    }

    private ShipmentAddress toAddress(AddressRequest r, AddressType type) {
        ShipmentAddress a = new ShipmentAddress();
        a.setAddressType(type);
        copyAddress(r, a);
        return a;
    }

    private void copyAddress(AddressRequest r, ShipmentAddress a) {
        a.setContactName(r.getContactName().trim());
        a.setContactPhone(r.getContactPhone());
        a.setContactEmail(AuthServiceImpl.blankToNull(r.getContactEmail()));
        a.setAddressLine1(r.getAddressLine1());
        a.setAddressLine2(AuthServiceImpl.blankToNull(r.getAddressLine2()));
        a.setLandmark(AuthServiceImpl.blankToNull(r.getLandmark()));
        a.setCity(r.getCity().trim());
        a.setState(r.getState().trim());
        a.setPincode(r.getPincode());
        a.setCountry(r.getCountry() == null || r.getCountry().isBlank() ? "India" : r.getCountry());
    }
}
