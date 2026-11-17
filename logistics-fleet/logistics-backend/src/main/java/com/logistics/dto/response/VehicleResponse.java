package com.logistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleResponse {
    private Long id;
    private String vehicleNumber;
    private String vehicleType;
    private String make;
    private String model;
    private Integer manufactureYear;
    private String fuelType;
    private BigDecimal capacityKg;
    private BigDecimal capacityCbm;
    private String chassisNumber;
    private String engineNumber;
    private String insuranceNumber;
    private LocalDate insuranceExpiry;
    private LocalDate permitExpiry;
    private LocalDate fitnessExpiry;
    private LocalDate pucExpiry;
    private Integer odometerKm;
    private Long homeWarehouseId;
    private String homeWarehouseName;
    private LocalDate lastServiceDate;
    private LocalDate nextServiceDate;
    private String status;
    private boolean insuranceValid;
    /** detail page only */
    private List<VehicleDocumentResponse> documents;
    private List<MaintenanceResponse> maintenanceHistory;
    private List<AssignmentResponse> recentTrips;

    public record VehicleDocumentResponse(Long id, String documentType, String documentNumber,
                                          String fileUrl, LocalDate expiryDate) {
    }
}
