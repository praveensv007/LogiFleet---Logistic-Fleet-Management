package com.logistics.dto.request;

import com.logistics.entity.enums.DriverAvailability;
import com.logistics.entity.enums.Gender;
import com.logistics.entity.enums.IdProofType;
import com.logistics.entity.enums.LicenseType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

import static com.logistics.dto.request.ValidationPatterns.*;

/** Create + update. username / temporaryPassword are only used on create. */
@Data
public class DriverRequest {

    @Pattern(regexp = "^$|" + USERNAME, message = "Username must be 4-30 letters, digits, dot or underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @Pattern(regexp = "^$|" + STRONG_PASSWORD, message = PASSWORD_MESSAGE)
    private String temporaryPassword;

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past
    private LocalDate dateOfBirth;

    private Gender gender;

    @NotBlank
    @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    @Pattern(regexp = PINCODE, message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "License number is required")
    @Size(min = 15, max = 16, message = "License number must be 15-16 characters")
    private String licenseNumber;

    @NotNull(message = "License type is required")
    private LicenseType licenseType;

    @NotNull(message = "License issue date is required")
    @PastOrPresent
    private LocalDate licenseIssueDate;

    @NotNull(message = "License expiry date is required")
    @Future(message = "License expiry must be after today")
    private LocalDate licenseExpiryDate;

    @NotBlank(message = "License copy is required")
    private String licenseDocUrl;

    @NotNull(message = "ID proof type is required")
    private IdProofType idProofType;

    @NotBlank(message = "ID proof number is required")
    @Size(max = 30)
    private String idProofNumber;

    @NotNull(message = "Experience is required")
    @Min(0)
    @Max(45)
    private Integer experienceYears;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @NotBlank(message = "Emergency contact name is required")
    private String emergencyContactName;

    @NotBlank
    @Pattern(regexp = PHONE, message = "Emergency phone must be 10 digits")
    private String emergencyContactPhone;

    private String photoUrl;

    @NotNull(message = "Availability is required")
    private DriverAvailability availabilityStatus;
}
