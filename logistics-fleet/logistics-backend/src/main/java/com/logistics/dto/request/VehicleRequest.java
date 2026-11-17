package com.logistics.dto.request;

import com.logistics.entity.enums.FuelType;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.entity.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.logistics.dto.request.ValidationPatterns.VEHICLE_NUMBER;

@Data
public class VehicleRequest {

    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = VEHICLE_NUMBER, message = "Use a registration like TN09AB1234")
    private String vehicleNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Make is required")
    @Size(max = 50)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    private String model;

    @NotNull(message = "Manufacture year is required")
    @Min(value = 1990, message = "Year must be 1990 or later")
    private Integer manufactureYear;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Capacity (kg) is required")
    @Positive(message = "Capacity must be greater than 0")
    private BigDecimal capacityKg;

    @Positive(message = "Capacity must be greater than 0")
    private BigDecimal capacityCbm;

    @NotBlank(message = "Chassis number is required")
    @Size(max = 30)
    private String chassisNumber;

    @NotBlank(message = "Engine number is required")
    @Size(max = 30)
    private String engineNumber;

    @NotBlank(message = "Insurance policy number is required")
    @Size(max = 30)
    private String insuranceNumber;

    @NotNull(message = "Insurance expiry is required")
    @Future(message = "Insurance expiry must be after today")
    private LocalDate insuranceExpiry;

    @NotNull(message = "Permit expiry is required")
    private LocalDate permitExpiry;

    @NotNull(message = "Fitness expiry is required")
    private LocalDate fitnessExpiry;

    @NotNull(message = "PUC expiry is required")
    private LocalDate pucExpiry;

    @NotNull(message = "Odometer is required")
    @PositiveOrZero
    private Integer odometerKm;

    private Long homeWarehouseId;

    @NotNull(message = "Status is required")
    private VehicleStatus status;
}
