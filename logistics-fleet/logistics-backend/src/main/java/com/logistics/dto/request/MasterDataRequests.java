package com.logistics.dto.request;

import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.DocumentType;
import com.logistics.entity.enums.DriverAvailability;
import com.logistics.entity.enums.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.logistics.dto.request.ValidationPatterns.PHONE;
import static com.logistics.dto.request.ValidationPatterns.PINCODE;

/** Smaller request bodies for drivers, vehicles and warehouses. */
public final class MasterDataRequests {

    private MasterDataRequests() {
    }

    @Data
    public static class Availability {
        @NotNull(message = "Availability is required")
        private DriverAvailability availabilityStatus;
    }

    @Data
    public static class VehicleStatusChange {
        @NotNull(message = "Status is required")
        private VehicleStatus status;
    }

    @Data
    public static class VehicleDocument {
        @NotNull(message = "Document type is required")
        private DocumentType documentType;

        @Size(max = 50)
        private String documentNumber;

        @NotBlank(message = "Upload the document file")
        private String fileUrl;

        private LocalDate expiryDate;
    }

    @Data
    public static class Warehouse {
        @NotBlank(message = "Warehouse code is required")
        @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "Use a code like WH-CHN-01")
        private String warehouseCode;

        @NotBlank
        @Size(min = 3, max = 100, message = "Name must be 3-100 characters")
        private String name;

        @NotBlank
        @Size(max = 200)
        private String addressLine1;

        @NotBlank
        private String city;

        @NotBlank
        private String state;

        @NotBlank
        @Pattern(regexp = PINCODE, message = "Pincode must be 6 digits")
        private String pincode;

        @NotBlank
        private String country;

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        private BigDecimal latitude;

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        private BigDecimal longitude;

        @NotNull(message = "Manager is required")
        private Long managerUserId;

        @NotBlank
        @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
        private String contactPhone;

        @NotBlank
        @Email
        private String contactEmail;

        @NotNull
        @Positive(message = "Capacity must be greater than 0")
        private Integer capacity;

        @NotNull(message = "Opening time is required")
        private LocalTime openingTime;

        @NotNull(message = "Closing time is required")
        private LocalTime closingTime;

        @NotNull
        private ActiveStatus status;
    }
}
