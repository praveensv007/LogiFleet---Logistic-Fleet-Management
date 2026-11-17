package com.logistics.entity;

import com.logistics.entity.enums.DriverAvailability;
import com.logistics.entity.enums.Gender;
import com.logistics.entity.enums.IdProofType;
import com.logistics.entity.enums.LicenseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "drivers")
public class Driver extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(unique = true, length = 20)
    private String driverCode;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false, unique = true, length = 20)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LicenseType licenseType;

    @Column(nullable = false)
    private LocalDate licenseIssueDate;

    @Column(nullable = false)
    private LocalDate licenseExpiryDate;

    private String licenseDocUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IdProofType idProofType;

    @Column(length = 30)
    private String idProofNumber;

    private Integer experienceYears;

    private LocalDate joiningDate;

    @Column(length = 100)
    private String emergencyContactName;

    @Column(length = 15)
    private String emergencyContactPhone;

    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DriverAvailability availabilityStatus = DriverAvailability.AVAILABLE;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLongitude;
}
