package com.logistics.dto.request;

import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.CustomerType;
import jakarta.validation.constraints.*;
import lombok.Data;

import static com.logistics.dto.request.ValidationPatterns.*;

@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
    private String phone;

    @Pattern(regexp = PHONE_OPTIONAL, message = "Alternate phone must be 10 digits")
    private String alternatePhone;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Size(max = 150)
    private String companyName;

    @Pattern(regexp = GSTIN, message = "Enter a valid 15-character GSTIN")
    private String gstNumber;

    @NotBlank
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    @Pattern(regexp = PINCODE, message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank
    private String country;

    /** Ignored when a customer edits their own profile. */
    private ActiveStatus status;
}
