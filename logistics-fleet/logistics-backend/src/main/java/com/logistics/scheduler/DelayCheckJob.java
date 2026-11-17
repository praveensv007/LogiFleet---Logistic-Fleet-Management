package com.logistics.scheduler;

import com.logistics.entity.Delivery;
import com.logistics.entity.FleetAssignment;
import com.logistics.entity.Shipment;
import com.logistics.entity.enums.AssignmentStatus;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.repository.DeliveryRepository;
import com.logistics.service.NotificationService;
import com.logistics.websocket.StatusUpdateMessage;
import com.logistics.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;

/** Every 15 minutes: mark deliveries past expected_delivery_time as delayed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayCheckJob {

    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final WebSocketPublisher publisher;

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int newlyDelayed = 0;
        for (Delivery d : deliveryRepository.findByAssignmentStatusIn(
                EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))) {
            FleetAssignment a = d.getAssignment();
            if (a.getExpectedDeliveryTime() == null || !now.isAfter(a.getExpectedDeliveryTime())) {
                continue;
            }
            d.setDelayMinutes((int) Duration.between(a.getExpectedDeliveryTime(), now).toMinutes());
            if (d.isDelayed()) {
                continue; // already notified, just refresh minutes
            }
            d.setDelayed(true);
            newlyDelayed++;
            Shipment s = d.getShipment();
            String msg = s.getTrackingNumber() + " passed its expected delivery time (" + a.getAssignmentCode()
                    + ", " + a.getDriver().getFullName() + ").";
            if (a.getAssignedBy() != null) {
                notificationService.notifyUser(a.getAssignedBy(), "Delivery delayed", msg,
                        NotificationType.SHIPMENT_DELAYED, ReferenceType.DELIVERY, d.getId());
            }
            if (s.getCustomer() != null && s.getCustomer().getUser() != null) {
                notificationService.notifyUser(s.getCustomer().getUser(), "Your shipment is running late",
                        s.getTrackingNumber() + " is taking longer than planned. We will keep you posted.",
                        NotificationType.SHIPMENT_DELAYED, ReferenceType.SHIPMENT, s.getId());
            }
            publisher.fleetActiveDeliveries(StatusUpdateMessage.builder().event("DELAY").deliveryId(d.getId())
                    .assignmentId(a.getId()).shipmentId(s.getId()).trackingNumber(s.getTrackingNumber())
                    .status("DELAYED").build());
        }
        if (newlyDelayed > 0) {
            log.info("DelayCheckJob: {} deliveries newly marked delayed", newlyDelayed);
        }
    }
}
