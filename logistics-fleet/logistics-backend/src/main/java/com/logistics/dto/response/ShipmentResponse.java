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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private Long originWarehouseId;
    private String originWarehouseName;
    private Long destinationWarehouseId;
    private String destinationWarehouseName;
    private String serviceType;
    private String description;
    private Integer totalPackages;
    private BigDecimal totalWeightKg;
    private BigDecimal declaredValue;
    private BigDecimal shippingCharge;
    private String paymentMode;
    private String specialInstructions;
    private LocalDate pickupDate;
    private String pickupTimeSlot;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String currentStatus;
    private String cancelReason;
    private LocalDateTime createdAt;
    private AddressResponse pickup;
    private AddressResponse delivery;
    private List<PackageResponse> packages;
    /** latest assignment, if any */
    private AssignmentResponse assignment;
    private String storageLocation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private Long id;
        private String addressType;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
        private String addressLine1;
        private String addressLine2;
        private String landmark;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageResponse {
        private Long id;
        private Long shipmentId;
        private String packageCode;
        private String description;
        private String packageType;
        private Integer quantity;
        private BigDecimal weightKg;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal volumetricWeightKg;
        private boolean fragile;
        private String packingType;
        private String currentStatus;
    }

    public record StatusHistoryResponse(Long id, String fromStatus, String status, LocalDateTime time,
                                        String location, String remarks, String changedBy) {
    }

    public record WarehouseTxnResponse(Long id, String transactionType, Integer packageCount, String conditionStatus,
                                       String storageLocation, String gatePassNumber, String packageCode,
                                       String performedBy, LocalDateTime transactionTime, String remarks) {
    }
}
