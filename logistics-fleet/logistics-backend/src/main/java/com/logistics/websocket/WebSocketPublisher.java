package com.logistics.websocket;

import com.logistics.dto.response.DashboardSummary;
import com.logistics.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * convertAndSend helpers for every topic in the spec. Messages are sent after the surrounding
 * transaction commits, so clients that refetch on a push always see the new data.
 */
@Slf4j
@Component
public class WebSocketPublisher {

    private final SimpMessagingTemplate template;

    public WebSocketPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void shipmentUpdate(String trackingNumber, StatusUpdateMessage msg) {
        send("/topic/shipments/" + trackingNumber, msg);
    }

    public void deliveryLocation(Long deliveryId, StatusUpdateMessage msg) {
        send("/topic/deliveries/" + deliveryId + "/location", msg);
    }

    public void fleetActiveDeliveries(StatusUpdateMessage msg) {
        send("/topic/fleet/active-deliveries", msg);
    }

    public void warehouse(Long warehouseId, StatusUpdateMessage msg) {
        if (warehouseId != null) {
            send("/topic/warehouse/" + warehouseId, msg);
        }
    }

    public void adminDashboard(DashboardSummary summary) {
        send("/topic/admin/dashboard", summary);
    }

    public void userNotification(String username, NotificationResponse notification) {
        afterCommit(() -> template.convertAndSendToUser(username, "/queue/notifications", notification));
    }

    private void send(String destination, Object payload) {
        afterCommit(() -> template.convertAndSend(destination, payload));
    }

    private void afterCommit(Runnable action) {
        Runnable safe = () -> {
            try {
                action.run();
            } catch (Exception e) {
                log.warn("WebSocket push failed: {}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }
}
