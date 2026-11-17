package com.logistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Public tracking view. Driver details appear after ASSIGNED, proof of delivery after DELIVERED. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {
    private String trackingNumber;
    private String currentStatus;
    private String serviceType;
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private String receiverName;
    private String originWarehouseName;
    private Integer totalPackages;
    private BigDecimal totalWeightKg;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime bookedAt;
    private List<ShipmentResponse.StatusHistoryResponse> timeline;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private Long deliveryId;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private boolean delayed;
    private String podReceiverName;
    private LocalDateTime deliveredTime;
    private String signatureUrl;
    private String podPhotoUrl;
}
