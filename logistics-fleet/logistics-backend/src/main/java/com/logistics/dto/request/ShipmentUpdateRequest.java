package com.logistics.dto.request;

import com.logistics.entity.enums.PackageStatus;
import com.logistics.entity.enums.PaymentMode;
import com.logistics.entity.enums.PickupTimeSlot;
import com.logistics.entity.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Editable while the shipment is still CREATED. */
@Data
public class ShipmentUpdateRequest {

    @NotNull
    @Valid
    private AddressRequest pickup;

    @NotNull
    @FutureOrPresent(message = "Pickup date cannot be in the past")
    private LocalDate pickupDate;

    @NotNull
    private PickupTimeSlot pickupTimeSlot;

    @NotNull
    @Valid
    private AddressRequest delivery;

    @NotNull
    private ServiceType serviceType;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    @PositiveOrZero
    private BigDecimal declaredValue;

    @NotNull
    private PaymentMode paymentMode;

    @Size(max = 500)
    private String specialInstructions;

    @Data
    public static class Reason {
        @NotBlank(message = "A reason is required")
        @Size(max = 255)
        private String reason;
    }

    @Data
    public static class PackageStatusChange {
        @NotNull(message = "Status is required")
        private PackageStatus status;

        @Size(max = 500)
        private String remarks;
    }
}
