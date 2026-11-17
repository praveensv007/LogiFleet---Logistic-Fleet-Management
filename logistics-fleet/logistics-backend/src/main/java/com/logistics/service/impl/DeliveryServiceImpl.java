package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.DeliveryStatusRequest;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.DeliveryResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.FleetMapper;
import com.logistics.repository.*;
import com.logistics.security.SecurityUtils;
import com.logistics.security.UserPrincipal;
import com.logistics.service.DeliveryService;
import com.logistics.service.NotificationService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private static final EnumSet<AssignmentStatus> ACTIVE = EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS);
    private static final int OTP_VALID_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FleetAssignmentRepository assignmentRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryProblemRepository problemRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final ShipmentStatusManager statusManager;
    private final NotificationService notificationService;
    private final WebSocketPublisher publisher;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.otp.dev-mode:false}")
    private boolean otpDevMode;

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> myAssignments() {
        Driver driver = currentDriver();
        return assignmentRepository.findByDriver_IdOrderByAssignedAtDesc(driver.getId(), PageRequest.of(0, 30))
                .stream().map(FleetMapper::toAssignment).toList();
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "DELIVERIES", entity = "FleetAssignment")
    public AssignmentResponse accept(Long assignmentId) {
        FleetAssignment a = ownAssignment(assignmentId);
        if (a.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new InvalidStatusTransitionException(a.getStatus(), AssignmentStatus.ACCEPTED);
        }
        a.setStatus(AssignmentStatus.ACCEPTED);
        a.setAcceptedAt(LocalDateTime.now());
        Delivery d = new Delivery();
        d.setAssignment(a);
        d.setShipment(a.getShipment());
        d.setDriver(a.getDriver());
        deliveryRepository.save(d);
        a.setDelivery(d);

        Shipment s = a.getShipment();
        String msg = a.getDriver().getFullName() + " accepted " + s.getTrackingNumber() + " on " + a.getVehicle().getVehicleNumber();
        if (a.getAssignedBy() != null) {
            notificationService.notifyUser(a.getAssignedBy(), "Trip accepted", msg,
                    NotificationType.SHIPMENT_STATUS_CHANGED, ReferenceType.ASSIGNMENT, a.getId());
        }
        Long warehouseId = s.getOriginWarehouse() != null ? s.getOriginWarehouse().getId() : null;
        notificationService.notifyWarehouse(warehouseId, "Ready to load: " + s.getTrackingNumber(),
                "Vehicle " + a.getVehicle().getVehicleNumber() + " (" + a.getDriver().getFullName() + ") is confirmed. Load and dispatch.",
                NotificationType.SHIPMENT_STATUS_CHANGED, ReferenceType.ASSIGNMENT, a.getId());
        StatusUpdateMessage m = event("READY_TO_LOAD", a, d, a.getStatus().name(), null);
        publisher.warehouse(warehouseId, m);
        publisher.fleetActiveDeliveries(m);
        return FleetMapper.toAssignment(a);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "DELIVERIES", entity = "FleetAssignment")
    public AssignmentResponse reject(Long assignmentId, String reason) {
        FleetAssignment a = ownAssignment(assignmentId);
        if (a.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new InvalidStatusTransitionException(a.getStatus(), AssignmentStatus.REJECTED);
        }
        a.setStatus(AssignmentStatus.REJECTED);
        a.setRejectionReason(reason);
        a.getDriver().setAvailabilityStatus(DriverAvailability.AVAILABLE);
        a.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        Shipment s = a.getShipment();
        statusManager.changeStatus(s, ShipmentStatus.READY_FOR_DISPATCH, null,
                "Driver rejected the trip; waiting for reassignment");
        if (a.getAssignedBy() != null) {
            notificationService.notifyUser(a.getAssignedBy(), "Trip rejected - reassign " + s.getTrackingNumber(),
                    a.getDriver().getFullName() + ": " + reason, NotificationType.PENDING_SHIPMENT,
                    ReferenceType.SHIPMENT, s.getId());
        }
        publisher.fleetActiveDeliveries(event("ASSIGNMENT", a, null, a.getStatus().name(), reason));
        return FleetMapper.toAssignment(a);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse get(Long deliveryId) {
        Delivery d = findDelivery(deliveryId);
        UserPrincipal p = SecurityUtils.current();
        if (!p.hasAuthority("DELIVERY_MONITOR") && !isOwnDriver(d, p)) {
            throw new ForbiddenException("This delivery belongs to another driver");
        }
        return detail(d);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "DELIVERIES", entity = "Delivery")
    public DeliveryResponse updateStatus(Long deliveryId, DeliveryStatusRequest r) {
        Delivery d = ownDelivery(deliveryId);
        FleetAssignment a = d.getAssignment();
        Shipment s = d.getShipment();
        DeliveryStatus from = d.getCurrentStatus();
        DeliveryStatus to = r.getStatus();
        ShipmentStatus shipmentTarget;
        switch (to) {
            case PICKED_UP -> {
                if (from != null) throw new InvalidStatusTransitionException(from, to);
                if (s.getCurrentStatus() != ShipmentStatus.DISPATCHED) {
                    throw new InvalidStatusTransitionException("Wait until the warehouse dispatches the shipment");
                }
                d.setPickupTime(LocalDateTime.now());
                d.setPickupPhotoUrl(AuthServiceImpl.blankToNull(r.getPickupPhotoUrl()));
                a.setStatus(AssignmentStatus.IN_PROGRESS);
                shipmentTarget = ShipmentStatus.PICKED_UP;
            }
            case IN_TRANSIT -> {
                if (from != DeliveryStatus.PICKED_UP && from != DeliveryStatus.OUT_FOR_DELIVERY) {
                    throw new InvalidStatusTransitionException(from, to);
                }
                shipmentTarget = ShipmentStatus.IN_TRANSIT;
            }
            case OUT_FOR_DELIVERY -> {
                if (from != DeliveryStatus.IN_TRANSIT) throw new InvalidStatusTransitionException(from, to);
                shipmentTarget = ShipmentStatus.OUT_FOR_DELIVERY;
            }
            default -> throw new BadRequestException("status", "Use the complete-delivery screen to finish the delivery");
        }
        d.setCurrentStatus(to);
        setLocation(d, r.getLatitude(), r.getLongitude());
        String remarks = r.getRemarks();
        if (to == DeliveryStatus.PICKED_UP && r.getPackagesPickedCount() != null) {
            remarks = (remarks == null ? "" : remarks + ". ") + "Picked " + r.getPackagesPickedCount() + " of " + s.getTotalPackages() + " packages";
        }
        saveHistory(d, to, r.getLatitude(), r.getLongitude(), r.getLocationText(), remarks);
        statusManager.changeStatus(s, shipmentTarget, r.getLocationText(), remarks);
        StatusUpdateMessage m = event("DELIVERY_STATUS", a, d, to.name(), remarks);
        m.setLocation(r.getLocationText());
        publisher.fleetActiveDeliveries(m);
        if (r.getLatitude() != null && r.getLongitude() != null) {
            publisher.deliveryLocation(d.getId(), m);
        }
        return detail(d);
    }

    @Override
    public DeliveryResponse updateLocation(Long deliveryId, DeliveryStatusRequest.Location r) {
        Delivery d = ownDelivery(deliveryId);
        if (!ACTIVE.contains(d.getAssignment().getStatus())) {
            throw new InvalidStatusTransitionException("Location can only be shared during an active trip");
        }
        setLocation(d, r.getLatitude(), r.getLongitude());
        StatusUpdateMessage m = event("LOCATION", d.getAssignment(), d,
                d.getCurrentStatus() != null ? d.getCurrentStatus().name() : null, null);
        m.setLatitude(r.getLatitude());
        m.setLongitude(r.getLongitude());
        m.setLocation(r.getLocationText());
        publisher.deliveryLocation(d.getId(), m);
        publisher.fleetActiveDeliveries(m);
        return FleetMapper.toDelivery(d, 0);
    }

    @Override
    public DeliveryResponse.OtpResponse sendOtp(Long deliveryId) {
        Delivery d = ownDelivery(deliveryId);
        if (d.getCurrentStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new InvalidStatusTransitionException("Mark the shipment OUT_FOR_DELIVERY before sending the OTP");
        }
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        d.setDeliveryOtpHash(passwordEncoder.encode(otp));
        d.setOtpGeneratedAt(LocalDateTime.now());
        d.setOtpVerified(false);
        Shipment s = d.getShipment();
        ShipmentAddress drop = s.getAddress(AddressType.DELIVERY);
        String text = "Your delivery OTP for " + s.getTrackingNumber() + " is " + otp
                + ". Share it with the driver only at delivery. Valid " + OTP_VALID_MINUTES + " minutes.";
        String sentTo = drop != null ? mask(drop.getContactPhone()) : "receiver";
        if (drop != null && drop.getContactEmail() != null) {
            emailService.send(drop.getContactEmail(), "Delivery OTP " + s.getTrackingNumber(), text);
            sentTo = sentTo + " / " + drop.getContactEmail();
        }
        // The booking customer also gets it in-app (they are often the receiver).
        if (s.getCustomer() != null && s.getCustomer().getUser() != null) {
            notificationService.notifyUser(s.getCustomer().getUser(), "Delivery OTP", text,
                    NotificationType.SHIPMENT_STATUS_CHANGED, ReferenceType.SHIPMENT, s.getId());
        }
        return new DeliveryResponse.OtpResponse(sentTo, otpDevMode ? otp : null);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "DELIVERIES", entity = "Delivery")
    public DeliveryResponse complete(Long deliveryId, DeliveryStatusRequest.Complete r) {
        Delivery d = ownDelivery(deliveryId);
        FleetAssignment a = d.getAssignment();
        Shipment s = d.getShipment();
        if (d.getCurrentStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new InvalidStatusTransitionException(d.getCurrentStatus(), DeliveryStatus.DELIVERED);
        }
        if (d.getDeliveryOtpHash() == null) {
            throw new BadRequestException("otp", "Send the OTP to the receiver first");
        }
        if (d.getOtpGeneratedAt().plusMinutes(OTP_VALID_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("otp", "OTP expired. Send a new one");
        }
        if (!passwordEncoder.matches(r.getOtp(), d.getDeliveryOtpHash())) {
            throw new BadRequestException("otp", "Incorrect OTP");
        }
        if (isBlank(r.getSignatureUrl()) && isBlank(r.getPodPhotoUrl())) {
            throw new BadRequestException("podPhotoUrl", "Capture a signature or a POD photo");
        }
        BigDecimal due = FleetMapper.codDue(s);
        if (s.getPaymentMode() == PaymentMode.COD) {
            if (r.getCodAmountCollected() == null || r.getCodAmountCollected().compareTo(due) < 0) {
                throw new BadRequestException("codAmountCollected", "Collect ₹" + due + " cash on delivery");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        d.setOtpVerified(true);
        d.setReceiverName(r.getReceiverName().trim());
        d.setReceiverPhone(r.getReceiverPhone());
        d.setSignatureUrl(AuthServiceImpl.blankToNull(r.getSignatureUrl()));
        d.setPodPhotoUrl(AuthServiceImpl.blankToNull(r.getPodPhotoUrl()));
        d.setCodAmountCollected(s.getPaymentMode() == PaymentMode.COD ? r.getCodAmountCollected() : null);
        d.setCurrentStatus(DeliveryStatus.DELIVERED);
        d.setDeliveredTime(now);
        if (a.getExpectedDeliveryTime() != null && now.isAfter(a.getExpectedDeliveryTime())) {
            d.setDelayed(true);
            d.setDelayMinutes((int) Duration.between(a.getExpectedDeliveryTime(), now).toMinutes());
        }
        saveHistory(d, DeliveryStatus.DELIVERED, d.getCurrentLatitude(), d.getCurrentLongitude(), null,
                "Delivered to " + d.getReceiverName());

        a.setStatus(AssignmentStatus.COMPLETED);
        a.setCompletedAt(now);
        a.getDriver().setAvailabilityStatus(DriverAvailability.AVAILABLE);
        a.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        s.getPackages().forEach(p -> statusManager.changePackageStatus(p, PackageStatus.DELIVERED, null, "Delivered"));
        statusManager.changeStatus(s, ShipmentStatus.DELIVERED, null, "Delivered to " + d.getReceiverName()
                + " (OTP verified)");

        if (a.getAssignedBy() != null) {
            notificationService.notifyUser(a.getAssignedBy(), "Delivered: " + s.getTrackingNumber(),
                    a.getDriver().getFullName() + " completed " + a.getAssignmentCode()
                            + (d.isDelayed() ? " (" + d.getDelayMinutes() + " min late)" : " on time"),
                    NotificationType.DELIVERY_COMPLETED, ReferenceType.ASSIGNMENT, a.getId());
        }
        publisher.fleetActiveDeliveries(event("DELIVERY_STATUS", a, d, DeliveryStatus.DELIVERED.name(), null));
        return detail(d);
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "DELIVERIES", entity = "DeliveryProblem")
    public DeliveryResponse.ProblemResponse reportProblem(Long deliveryId, DeliveryStatusRequest.Problem r) {
        Delivery d = ownDelivery(deliveryId);
        DeliveryProblem p = new DeliveryProblem();
        p.setDelivery(d);
        p.setReportedBy(userRepository.getReferenceById(SecurityUtils.current().getId()));
        p.setProblemType(r.getProblemType());
        p.setDescription(r.getDescription().trim());
        p.setPhotoUrl(AuthServiceImpl.blankToNull(r.getPhotoUrl()));
        p.setLatitude(r.getLatitude());
        p.setLongitude(r.getLongitude());
        p.setReportedAt(LocalDateTime.now());
        p.setResolutionStatus(ResolutionStatus.OPEN);
        problemRepository.save(p);
        if (r.getLatitude() != null && r.getLongitude() != null) {
            setLocation(d, r.getLatitude(), r.getLongitude());
        }
        String title = "Problem: " + r.getProblemType().name().replace('_', ' ') + " on " + d.getShipment().getTrackingNumber();
        notificationService.notifyRole(RoleName.FLEET_MANAGER, title,
                d.getDriver().getFullName() + ": " + r.getDescription(), NotificationType.PROBLEM_REPORTED,
                ReferenceType.DELIVERY, d.getId());
        publisher.fleetActiveDeliveries(event("PROBLEM", d.getAssignment(), d, r.getProblemType().name(), r.getDescription()));
        return FleetMapper.toProblem(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponse> active() {
        return deliveryRepository.findByAssignmentStatusIn(ACTIVE).stream()
                .map(d -> FleetMapper.toDelivery(d,
                        problemRepository.countByDelivery_IdAndResolutionStatusNot(d.getId(), ResolutionStatus.RESOLVED)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponse.ProblemResponse> problems(ResolutionStatus status) {
        List<DeliveryProblem> list = status == null ? problemRepository.findAllByOrderByReportedAtDesc()
                : problemRepository.findAllByOrderByReportedAtDesc().stream()
                .filter(p -> p.getResolutionStatus() == status).toList();
        return list.stream().map(FleetMapper::toProblem).toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "DELIVERIES", entity = "DeliveryProblem")
    public DeliveryResponse.ProblemResponse resolveProblem(Long problemId, DeliveryStatusRequest.Resolve r) {
        DeliveryProblem p = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));
        p.setResolutionStatus(r.getResolutionStatus());
        p.setResolutionNotes(AuthServiceImpl.blankToNull(r.getResolutionNotes()));
        if (r.getResolutionStatus() == ResolutionStatus.RESOLVED) {
            p.setResolvedBy(userRepository.getReferenceById(SecurityUtils.current().getId()));
            p.setResolvedAt(LocalDateTime.now());
        }
        User driverUser = p.getDelivery().getDriver().getUser();
        if (driverUser != null) {
            notificationService.notifyUser(driverUser, "Problem " + r.getResolutionStatus().name().replace('_', ' ').toLowerCase(),
                    p.getProblemType() + (p.getResolutionNotes() != null ? ": " + p.getResolutionNotes() : ""),
                    NotificationType.PROBLEM_REPORTED, ReferenceType.DELIVERY, p.getDelivery().getId());
        }
        publisher.fleetActiveDeliveries(event("PROBLEM", p.getDelivery().getAssignment(), p.getDelivery(),
                r.getResolutionStatus().name(), null));
        return FleetMapper.toProblem(p);
    }

    // ---------- helpers ----------

    private DeliveryResponse detail(Delivery d) {
        DeliveryResponse res = FleetMapper.toDelivery(d,
                problemRepository.countByDelivery_IdAndResolutionStatusNot(d.getId(), ResolutionStatus.RESOLVED));
        res.setHistory(historyRepository.findByDelivery_IdOrderByIdAsc(d.getId()).stream().map(FleetMapper::toHistory).toList());
        res.setProblems(problemRepository.findByDelivery_IdOrderByReportedAtDesc(d.getId()).stream().map(FleetMapper::toProblem).toList());
        return res;
    }

    private void setLocation(Delivery d, BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return;
        }
        d.setCurrentLatitude(lat);
        d.setCurrentLongitude(lng);
        d.setLastLocationUpdate(LocalDateTime.now());
        d.getDriver().setCurrentLatitude(lat);
        d.getDriver().setCurrentLongitude(lng);
    }

    private void saveHistory(Delivery d, DeliveryStatus status, BigDecimal lat, BigDecimal lng, String text, String remarks) {
        DeliveryStatusHistory h = new DeliveryStatusHistory();
        h.setDelivery(d);
        h.setStatus(status);
        h.setLatitude(lat);
        h.setLongitude(lng);
        h.setLocationText(text);
        h.setRemarks(remarks);
        historyRepository.save(h);
    }

    private StatusUpdateMessage event(String type, FleetAssignment a, Delivery d, String status, String remarks) {
        return StatusUpdateMessage.builder()
                .event(type)
                .assignmentId(a.getId())
                .deliveryId(d != null ? d.getId() : null)
                .shipmentId(a.getShipment().getId())
                .trackingNumber(a.getShipment().getTrackingNumber())
                .status(status)
                .latitude(d != null ? d.getCurrentLatitude() : null)
                .longitude(d != null ? d.getCurrentLongitude() : null)
                .remarks(remarks)
                .build();
    }

    private Driver currentDriver() {
        return driverRepository.findByUser_Id(SecurityUtils.current().getId())
                .orElseThrow(() -> new ForbiddenException("Only drivers can do this"));
    }

    private FleetAssignment ownAssignment(Long id) {
        FleetAssignment a = assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
        if (!a.getDriver().getId().equals(currentDriver().getId())) {
            throw new ForbiddenException("This trip is assigned to another driver");
        }
        return a;
    }

    private Delivery ownDelivery(Long id) {
        Delivery d = findDelivery(id);
        if (!d.getDriver().getId().equals(currentDriver().getId())) {
            throw new ForbiddenException("This delivery belongs to another driver");
        }
        return d;
    }

    private Delivery findDelivery(Long id) {
        return deliveryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
    }

    private static boolean isOwnDriver(Delivery d, UserPrincipal p) {
        return d.getDriver().getUser() != null && d.getDriver().getUser().getId().equals(p.getId());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String mask(String phone) {
        return phone == null || phone.length() < 4 ? "receiver" : "******" + phone.substring(phone.length() - 4);
    }
}
