package com.logistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryResponse {
    private Long id;
    private Long assignmentId;
    private String assignmentCode;
    private String assignmentStatus;
    private Long shipmentId;
    private String trackingNumber;
    private String shipmentStatus;
    private Integer totalPackages;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String currentStatus;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveredTime;
    private String pickupPhotoUrl;
    private String receiverName;
    private String receiverPhone;
    private boolean otpVerified;
    private boolean otpSent;
    private String signatureUrl;
    private String podPhotoUrl;
    private String paymentMode;
    private BigDecimal codAmountDue;
    private BigDecimal codAmountCollected;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private boolean delayed;
    private Integer delayMinutes;
    private LocalDateTime expectedDeliveryTime;
    private ShipmentResponse.AddressResponse pickupAddress;
    private ShipmentResponse.AddressResponse deliveryAddress;
    private long openProblems;
    private List<HistoryItem> history;
    private List<ProblemResponse> problems;

    public record HistoryItem(String status, BigDecimal latitude, BigDecimal longitude,
                              String locationText, String remarks, LocalDateTime time) {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemResponse {
        private Long id;
        private Long deliveryId;
        private String trackingNumber;
        private String driverName;
        private String vehicleNumber;
        private String problemType;
        private String description;
        private String photoUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime reportedAt;
        private String resolutionStatus;
        private String resolutionNotes;
        private String resolvedByName;
        private LocalDateTime resolvedAt;
    }

    /** POST /deliveries/{id}/send-otp. devOtp is only filled when app.otp.dev-mode=true. */
    public record OtpResponse(String sentTo, String devOtp) {
    }
}
