package com.logistics.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.logistics.dto.request.ValidationPatterns.PHONE;
import static com.logistics.dto.request.ValidationPatterns.PINCODE;

/** Pickup or delivery address of a shipment. */
@Data
public class AddressRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 100)
    private String contactName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
    private String contactPhone;

    @Email(message = "Enter a valid email")
    @Size(max = 100)
    private String contactEmail;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @Size(max = 150)
    private String landmark;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = PINCODE, message = "Pincode must be 6 digits")
    private String pincode;

    private String country = "India";
}
