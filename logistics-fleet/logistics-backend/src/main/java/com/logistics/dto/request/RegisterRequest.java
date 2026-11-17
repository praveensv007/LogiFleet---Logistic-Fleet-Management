package com.logistics.dto.request;

import com.logistics.entity.enums.CustomerType;
import jakarta.validation.constraints.*;
import lombok.Data;

import static com.logistics.dto.request.ValidationPatterns.*;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = USERNAME, message = "Username must be 4-30 letters, digits, dot or underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = STRONG_PASSWORD, message = PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "Confirm your password")
    private String confirmPassword;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Size(max = 150)
    private String companyName;

    @Pattern(regexp = GSTIN, message = "Enter a valid 15-character GSTIN")
    private String gstNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 60)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 60)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = PINCODE, message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "Country is required")
    private String country;

    @AssertTrue(message = "You must accept the terms")
    private boolean acceptTerms;
}
