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
public class MaintenanceResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleNumber;
    private String maintenanceType;
    private String description;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private Integer odometerKm;
    private String serviceCenter;
    private BigDecimal labourCost;
    private BigDecimal totalCost;
    private LocalDate nextServiceDate;
    private Integer nextServiceOdometer;
    private String invoiceUrl;
    private String status;
    private boolean overdue;
    private List<PartResponse> parts;

    public record PartResponse(Long id, String partName, String partNumber, Integer quantity,
                               BigDecimal unitCost, BigDecimal totalCost) {
    }
}
