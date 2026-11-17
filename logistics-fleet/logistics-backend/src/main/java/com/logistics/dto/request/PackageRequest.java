package com.logistics.dto.request;

import com.logistics.entity.enums.PackageType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequest {

    @NotBlank(message = "Package description is required")
    @Size(max = 200)
    private String description;

    @NotNull(message = "Package type is required")
    private PackageType packageType;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull
    @Positive(message = "Weight must be greater than 0")
    private BigDecimal weightKg;

    @NotNull
    @Positive(message = "Length must be greater than 0")
    private BigDecimal lengthCm;

    @NotNull
    @Positive(message = "Width must be greater than 0")
    private BigDecimal widthCm;

    @NotNull
    @Positive(message = "Height must be greater than 0")
    private BigDecimal heightCm;

    private boolean fragile;
}
