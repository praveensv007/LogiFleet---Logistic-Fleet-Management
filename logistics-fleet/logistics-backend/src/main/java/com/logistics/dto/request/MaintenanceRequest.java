package com.logistics.dto.request;

import com.logistics.entity.enums.MaintenanceStatus;
import com.logistics.entity.enums.MaintenanceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaintenanceRequest {

    @NotNull(message = "Select a vehicle")
    private Long vehicleId;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    @NotBlank(message = "Description is required")
    @Size(max = 500)
    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private LocalDate completedDate;

    @NotNull(message = "Odometer is required")
    @PositiveOrZero
    private Integer odometerKm;

    @Size(max = 100)
    private String serviceCenter;

    @PositiveOrZero
    private BigDecimal labourCost;

    @PositiveOrZero
    private BigDecimal totalCost;

    private LocalDate nextServiceDate;

    @PositiveOrZero
    private Integer nextServiceOdometer;

    @NotNull(message = "Status is required")
    private MaintenanceStatus status;

    private String invoiceUrl;

    @Valid
    private List<Part> parts = new ArrayList<>();

    @Data
    public static class Part {
        @NotBlank(message = "Part name is required")
        @Size(max = 100)
        private String partName;

        @Size(max = 50)
        private String partNumber;

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull
        @PositiveOrZero
        private BigDecimal unitCost;
    }

    @Data
    public static class Complete {
        @NotNull(message = "Completed date is required")
        private LocalDate completedDate;

        @PositiveOrZero
        private BigDecimal totalCost;

        private LocalDate nextServiceDate;
        private Integer nextServiceOdometer;
    }
}
