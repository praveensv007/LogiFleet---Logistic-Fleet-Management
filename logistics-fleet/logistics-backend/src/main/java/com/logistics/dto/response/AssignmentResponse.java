package com.logistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentResponse {
    private Long id;
    private String assignmentCode;
    private Long shipmentId;
    private String trackingNumber;
    private String shipmentStatus;
    private String serviceType;
    private String paymentMode;
    private Integer totalPackages;
    private BigDecimal totalWeightKg;
    private BigDecimal codAmountDue;
    private String pickupCity;
    private String deliveryCity;
    private String deliveryAddress;
    private String receiverName;
    private String receiverPhone;
    private Long originWarehouseId;
    private String originWarehouseName;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Long vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private String assignedByName;
    private LocalDateTime assignedAt;
    private LocalDateTime plannedStartTime;
    private LocalDateTime expectedDeliveryTime;
    private String routeNotes;
    private String status;
    private String rejectionReason;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private Long deliveryId;
    private String deliveryStatus;
    private boolean delayed;
    private Integer delayMinutes;
}
