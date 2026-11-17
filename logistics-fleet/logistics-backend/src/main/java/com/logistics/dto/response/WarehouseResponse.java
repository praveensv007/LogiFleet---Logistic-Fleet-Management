package com.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String warehouseCode;
    private String name;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long managerUserId;
    private String managerName;
    private String contactPhone;
    private String contactEmail;
    private Integer capacity;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String status;
    private long pendingShipments;
}
