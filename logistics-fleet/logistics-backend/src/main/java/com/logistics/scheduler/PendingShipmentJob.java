package com.logistics.scheduler;

import com.logistics.entity.Shipment;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.ShipmentStatus;
import com.logistics.repository.ShipmentRepository;
import com.logistics.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Daily 08:00: shipments still CREATED after 24 hours. */
@Component
@RequiredArgsConstructor
public class PendingShipmentJob {

    private final ShipmentRepository shipmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void run() {
        List<Shipment> stale = shipmentRepository.findByCurrentStatusAndCreatedAtBefore(
                ShipmentStatus.CREATED, LocalDateTime.now().minusHours(24));
        if (stale.isEmpty()) {
            return;
        }
        Map<Long, List<Shipment>> byWarehouse = stale.stream().filter(s -> s.getOriginWarehouse() != null)
                .collect(Collectors.groupingBy(s -> s.getOriginWarehouse().getId()));
        byWarehouse.forEach((warehouseId, list) -> notificationService.notifyWarehouse(warehouseId,
                list.size() + " shipment(s) waiting over 24 hours",
                list.stream().map(Shipment::getTrackingNumber).limit(10).collect(Collectors.joining(", ")),
                NotificationType.PENDING_SHIPMENT, ReferenceType.SHIPMENT, list.get(0).getId()));
        notificationService.notifyRole(RoleName.ADMIN, "Pending shipments",
                stale.size() + " shipment(s) have not been received for more than 24 hours.",
                NotificationType.PENDING_SHIPMENT, ReferenceType.SHIPMENT, stale.get(0).getId());
    }
}
