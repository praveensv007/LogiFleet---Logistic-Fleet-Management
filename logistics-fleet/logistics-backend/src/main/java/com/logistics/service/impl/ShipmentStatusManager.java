package com.logistics.service.impl;

import com.logistics.entity.*;
import com.logistics.entity.enums.*;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.repository.PackageStatusHistoryRepository;
import com.logistics.repository.ShipmentStatusHistoryRepository;
import com.logistics.repository.UserRepository;
import com.logistics.security.SecurityUtils;
import com.logistics.service.AuditService;
import com.logistics.service.NotificationService;
import com.logistics.service.ReportService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The single place where a shipment / package status changes: validates the transition,
 * writes the *_status_history row, audits it, notifies the customer and pushes the WebSocket update.
 */
@Component
@RequiredArgsConstructor
public class ShipmentStatusManager {

    private final ShipmentStatusHistoryRepository historyRepository;
    private final PackageStatusHistoryRepository packageHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final WebSocketPublisher publisher;
    private final ReportService reportService;

    /** First history row when the shipment is booked. */
    public void recordCreated(Shipment s) {
        saveHistory(s, null, ShipmentStatus.CREATED,
                s.getOriginWarehouse() != null ? s.getOriginWarehouse().getCity() : null, "Shipment booked");
        publisher.shipmentUpdate(s.getTrackingNumber(), message(s, null, "Shipment booked"));
        pushDashboard();
    }

    public void changeStatus(Shipment s, ShipmentStatus to, String location, String remarks) {
        ShipmentStatus from = s.getCurrentStatus();
        if (from == to) {
            return;
        }
        if (!from.canTransitionTo(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
        s.setCurrentStatus(to);
        if (to == ShipmentStatus.DELIVERED) {
            s.setActualDeliveryDate(LocalDateTime.now());
        }
        saveHistory(s, from, to, location, remarks);
        auditService.log(AuditAction.STATUS_CHANGE, "SHIPMENT", "Shipment", s.getId(),
                Map.of("status", from.name()), Map.of("status", to.name()));

        if (s.getCustomer() != null && s.getCustomer().getUser() != null) {
            notificationService.notifyUser(s.getCustomer().getUser(),
                    "Shipment " + s.getTrackingNumber() + ": " + pretty(to),
                    remarks != null ? remarks : "Your shipment is now " + pretty(to).toLowerCase() + ".",
                    to == ShipmentStatus.DELIVERED ? NotificationType.DELIVERY_COMPLETED : NotificationType.SHIPMENT_STATUS_CHANGED,
                    ReferenceType.SHIPMENT, s.getId());
        }
        publisher.shipmentUpdate(s.getTrackingNumber(), message(s, location, remarks));
        publisher.fleetActiveDeliveries(message(s, location, remarks));
        if (s.getOriginWarehouse() != null) {
            publisher.warehouse(s.getOriginWarehouse().getId(), message(s, location, remarks));
        }
        pushDashboard();
    }

    public void changePackageStatus(ShipmentPackage p, PackageStatus to, Warehouse warehouse, String remarks) {
        PackageStatus from = p.getCurrentStatus();
        if (from == to) {
            return;
        }
        p.setCurrentStatus(to);
        PackageStatusHistory h = new PackageStatusHistory();
        h.setShipmentPackage(p);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setWarehouse(warehouse);
        h.setRemarks(remarks);
        h.setChangedBy(currentUser());
        h.setChangedAt(LocalDateTime.now());
        packageHistoryRepository.save(h);
    }

    public void pushDashboard() {
        publisher.adminDashboard(reportService.summary());
    }

    private void saveHistory(Shipment s, ShipmentStatus from, ShipmentStatus to, String location, String remarks) {
        ShipmentStatusHistory h = new ShipmentStatusHistory();
        h.setShipment(s);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setLocation(location);
        h.setRemarks(remarks);
        h.setChangedBy(currentUser());
        h.setChangedAt(LocalDateTime.now());
        historyRepository.save(h);
    }

    private StatusUpdateMessage message(Shipment s, String location, String remarks) {
        return StatusUpdateMessage.builder()
                .event("SHIPMENT_STATUS")
                .shipmentId(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getCurrentStatus().name())
                .location(location)
                .remarks(remarks)
                .build();
    }

    private User currentUser() {
        Long id = SecurityUtils.currentUserId();
        return id == null ? null : userRepository.getReferenceById(id);
    }

    public static String pretty(Enum<?> e) {
        String s = e.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
