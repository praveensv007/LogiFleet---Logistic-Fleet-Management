package com.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String username;
    private String customerCode;
    private String fullName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String customerType;
    private String companyName;
    private String gstNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String status;
    private long totalShipments;
    private LocalDateTime createdAt;
}
