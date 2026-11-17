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
public class DriverResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String userStatus;
    private String driverCode;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String licenseNumber;
    private String licenseType;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;
    private boolean licenseValid;
    private String licenseDocUrl;
    private String idProofType;
    private String idProofNumber;
    private Integer experienceYears;
    private LocalDate joiningDate;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String photoUrl;
    private String availabilityStatus;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    /** detail page only */
    private List<AssignmentResponse> recentTrips;
}
