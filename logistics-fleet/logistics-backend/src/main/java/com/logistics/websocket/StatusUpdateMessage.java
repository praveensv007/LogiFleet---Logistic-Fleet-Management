package com.logistics.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Payload pushed on every /topic/** update. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusUpdateMessage {
    /** SHIPMENT_STATUS, LOCATION, DELIVERY_STATUS, PROBLEM, DELAY, NEW_SHIPMENT, READY_TO_LOAD, ASSIGNMENT */
    private String event;
    private Long shipmentId;
    private String trackingNumber;
    private Long deliveryId;
    private Long assignmentId;
    private String status;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String remarks;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
