package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.AssignmentRequest;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.FleetMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.*;
import com.logistics.security.SecurityUtils;
import com.logistics.service.AssignmentService;
import com.logistics.service.NotificationService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final FleetAssignmentRepository assignmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final TrackingNumberGenerator generator;
    private final ShipmentStatusManager statusManager;
    private final NotificationService notificationService;
    private final WebSocketPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignmentResponse> list(AssignmentStatus status, Long driverId, String search,
                                                 int page, int size, String sort) {
        Specification<FleetAssignment> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (driverId != null) ps.add(cb.equal(root.get("driver").get("id"), driverId));
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                Join<FleetAssignment, Shipment> s = root.join("shipment");
                Join<FleetAssignment, Driver> d = root.join("driver");
                Join<FleetAssignment, Vehicle> v = root.join("vehicle");
                ps.add(cb.or(cb.like(cb.lower(s.get("trackingNumber")), like),
                        cb.like(cb.lower(root.get("assignmentCode")), like),
                        cb.like(cb.lower(d.get("fullName")), like),
                        cb.like(cb.lower(v.get("vehicleNumber")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(assignmentRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "assignedAt")),
                FleetMapper::toAssignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> pendingShipments() {
        return shipmentRepository.findByCurrentStatusIn(EnumSet.of(ShipmentStatus.READY_FOR_DISPATCH),
                        Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .filter(s -> !assignmentRepository.existsByShipment_IdAndStatusIn(s.getId(), ShipmentServiceImpl.OPEN_ASSIGNMENT))
                .map(s -> ShipmentMapper.toResponse(s, false, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse get(Long id) {
        return FleetMapper.toAssignment(find(id));
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "ASSIGNMENTS", entity = "FleetAssignment")
    public AssignmentResponse create(AssignmentRequest r) {
        Shipment s = shipmentRepository.findById(r.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", r.getShipmentId()));
        if (s.getCurrentStatus() != ShipmentStatus.READY_FOR_DISPATCH) {
            throw new BadRequestException("shipmentId", "Only READY_FOR_DISPATCH shipments can be assigned (this one is "
                    + s.getCurrentStatus() + ")");
        }
        if (assignmentRepository.existsByShipment_IdAndStatusIn(s.getId(), ShipmentServiceImpl.OPEN_ASSIGNMENT)) {
            throw new BadRequestException("shipmentId", "This shipment already has an open assignment");
        }
        Driver driver = driverRepository.findById(r.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", r.getDriverId()));
        if (driver.getAvailabilityStatus() != DriverAvailability.AVAILABLE) {
            throw new BadRequestException("driverId", driver.getFullName() + " is " + driver.getAvailabilityStatus());
        }
        if (driver.getLicenseExpiryDate() == null || !driver.getLicenseExpiryDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("driverId", driver.getFullName() + "'s license has expired");
        }
        Vehicle vehicle = vehicleRepository.findById(r.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", r.getVehicleId()));
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BadRequestException("vehicleId", vehicle.getVehicleNumber() + " is " + vehicle.getStatus());
        }
        if (vehicle.getCapacityKg().compareTo(s.getTotalWeightKg()) < 0) {
            throw new BadRequestException("vehicleId", "Capacity " + vehicle.getCapacityKg() + " kg is less than shipment weight "
                    + s.getTotalWeightKg() + " kg");
        }
        if (vehicle.getInsuranceExpiry() == null || !vehicle.getInsuranceExpiry().isAfter(LocalDate.now())) {
            throw new BadRequestException("vehicleId", vehicle.getVehicleNumber() + "'s insurance has expired");
        }
        validateTimes(r.getPlannedStartTime(), r.getExpectedDeliveryTime());

        FleetAssignment a = new FleetAssignment();
        a.setAssignmentCode(generator.nextAssignmentCode());
        a.setShipment(s);
        a.setDriver(driver);
        a.setVehicle(vehicle);
        a.setAssignedBy(userRepository.getReferenceById(SecurityUtils.current().getId()));
        a.setAssignedAt(LocalDateTime.now());
        a.setPlannedStartTime(r.getPlannedStartTime());
        a.setExpectedDeliveryTime(r.getExpectedDeliveryTime());
        a.setRouteNotes(AuthServiceImpl.blankToNull(r.getRouteNotes()));
        a.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(a);

        driver.setAvailabilityStatus(DriverAvailability.ON_TRIP);
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        statusManager.changeStatus(s, ShipmentStatus.ASSIGNED, null,
                "Assigned to " + driver.getFullName() + " (" + vehicle.getVehicleNumber() + ")");

        if (driver.getUser() != null) {
            notificationService.notifyUser(driver.getUser(), "New trip " + a.getAssignmentCode(),
                    s.getTrackingNumber() + ": " + s.getTotalPackages() + " pkg, " + s.getTotalWeightKg() + " kg. Start "
                            + r.getPlannedStartTime().format(DateTimeFormatter.ofPattern("dd MMM hh:mm a"))
                            + ". Accept or reject in My Deliveries.",
                    NotificationType.ASSIGNMENT_NEW, ReferenceType.ASSIGNMENT, a.getId());
        }
        publisher.fleetActiveDeliveries(StatusUpdateMessage.builder().event("ASSIGNMENT")
                .assignmentId(a.getId()).shipmentId(s.getId()).trackingNumber(s.getTrackingNumber())
                .status(a.getStatus().name()).build());
        return FleetMapper.toAssignment(a);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "ASSIGNMENTS", entity = "FleetAssignment")
    public AssignmentResponse update(Long id, AssignmentRequest.Update r) {
        FleetAssignment a = find(id);
        if (a.getStatus() != AssignmentStatus.ASSIGNED && a.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new InvalidStatusTransitionException("Only ASSIGNED or ACCEPTED trips can be edited");
        }
        if (r.getExpectedDeliveryTime() == null || !r.getExpectedDeliveryTime().isAfter(r.getPlannedStartTime())) {
            throw new BadRequestException("expectedDeliveryTime", "Expected delivery must be after planned start");
        }
        a.setPlannedStartTime(r.getPlannedStartTime());
        a.setExpectedDeliveryTime(r.getExpectedDeliveryTime());
        a.setRouteNotes(AuthServiceImpl.blankToNull(r.getRouteNotes()));
        return FleetMapper.toAssignment(a);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "ASSIGNMENTS", entity = "FleetAssignment")
    public AssignmentResponse cancel(Long id, String reason) {
        FleetAssignment a = find(id);
        if (a.getStatus() != AssignmentStatus.ASSIGNED && a.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new InvalidStatusTransitionException("Only trips that have not been picked up can be cancelled");
        }
        Shipment s = a.getShipment();
        a.setStatus(AssignmentStatus.CANCELLED);
        a.setRejectionReason(reason);
        a.getDriver().setAvailabilityStatus(DriverAvailability.AVAILABLE);
        a.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        // back to the warehouse: unload anything already loaded
        s.getPackages().stream().filter(p -> p.getCurrentStatus() == PackageStatus.LOADED)
                .forEach(p -> statusManager.changePackageStatus(p, PackageStatus.PACKED, s.getOriginWarehouse(),
                        "Trip cancelled, back in warehouse"));
        if (s.getCurrentStatus() == ShipmentStatus.ASSIGNED || s.getCurrentStatus() == ShipmentStatus.DISPATCHED) {
            statusManager.changeStatus(s, ShipmentStatus.READY_FOR_DISPATCH, null, "Trip cancelled: " + reason);
        }
        if (a.getDriver().getUser() != null) {
            notificationService.notifyUser(a.getDriver().getUser(), "Trip " + a.getAssignmentCode() + " cancelled",
                    reason, NotificationType.SHIPMENT_STATUS_CHANGED, ReferenceType.ASSIGNMENT, a.getId());
        }
        return FleetMapper.toAssignment(a);
    }

    private void validateTimes(LocalDateTime start, LocalDateTime expected) {
        if (start.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("plannedStartTime", "Planned start cannot be in the past");
        }
        if (!expected.isAfter(start)) {
            throw new BadRequestException("expectedDeliveryTime", "Expected delivery must be after planned start");
        }
    }

    private FleetAssignment find(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }
}
