package com.logistics.dto.request;

import com.logistics.entity.enums.DeliveryStatus;
import com.logistics.entity.enums.ProblemType;
import com.logistics.entity.enums.ResolutionStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

import static com.logistics.dto.request.ValidationPatterns.PHONE;

/** Driver: PICKED_UP / IN_TRANSIT / OUT_FOR_DELIVERY with location. Nested classes hold the other driver actions. */
@Data
public class DeliveryStatusRequest {

    @NotNull(message = "New status is required")
    private DeliveryStatus status;

    @Size(max = 150)
    private String locationText;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 500)
    private String remarks;

    /** Pickup completed */
    @Min(0)
    private Integer packagesPickedCount;

    private String pickupPhotoUrl;

    @Data
    public static class Location {
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private BigDecimal latitude;

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private BigDecimal longitude;

        @Size(max = 150)
        private String locationText;
    }

    @Data
    public static class Complete {
        @NotBlank(message = "Receiver name is required")
        @Size(max = 100)
        private String receiverName;

        @NotBlank(message = "Receiver phone is required")
        @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
        private String receiverPhone;

        @NotBlank(message = "Delivery OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP is 6 digits")
        private String otp;

        private String signatureUrl;
        private String podPhotoUrl;

        @PositiveOrZero
        private BigDecimal codAmountCollected;
    }

    @Data
    public static class Problem {
        @NotNull(message = "Problem type is required")
        private ProblemType problemType;

        @NotBlank(message = "Description is required")
        @Size(max = 500)
        private String description;

        private String photoUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Data
    public static class Resolve {
        @NotNull
        private ResolutionStatus resolutionStatus;

        @Size(max = 500)
        private String resolutionNotes;
    }
}
