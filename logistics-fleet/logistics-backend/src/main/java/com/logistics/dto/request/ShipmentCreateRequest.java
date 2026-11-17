package com.logistics.dto.request;

import com.logistics.entity.enums.PaymentMode;
import com.logistics.entity.enums.PickupTimeSlot;
import com.logistics.entity.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ShipmentCreateRequest {

    /** Only used when staff (admin) books for a customer; customers always book for themselves. */
    private Long customerId;

    @NotNull(message = "Pickup details are required")
    @Valid
    private AddressRequest pickup;

    @NotNull(message = "Pickup date is required")
    @FutureOrPresent(message = "Pickup date cannot be in the past")
    private LocalDate pickupDate;

    @NotNull(message = "Pickup time slot is required")
    private PickupTimeSlot pickupTimeSlot;

    @NotNull(message = "Delivery details are required")
    @Valid
    private AddressRequest delivery;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Shipment description is required")
    @Size(max = 500)
    private String description;

    @NotNull(message = "Declared value is required")
    @PositiveOrZero(message = "Declared value must be 0 or more")
    private BigDecimal declaredValue;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(max = 500)
    private String specialInstructions;

    /** Optional: nearest active warehouse is chosen when empty. */
    private Long originWarehouseId;

    @NotEmpty(message = "Add at least one package")
    @Valid
    private List<PackageRequest> packages;
}
