package com.logistics.dto.request;

import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

import static com.logistics.dto.request.ValidationPatterns.*;

/** Request bodies for user management, roles and the profile screen. */
public final class UserRequests {

    private UserRequests() {
    }

    @Data
    public static class Create {
        @NotBlank(message = "Full name is required")
        @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
        private String fullName;

        @NotBlank(message = "Username is required")
        @Pattern(regexp = USERNAME, message = "Username must be 4-30 letters, digits, dot or underscore")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
        private String phone;

        @NotNull(message = "Role is required")
        private RoleName role;

        /** Required when role is WAREHOUSE_STAFF */
        private Long warehouseId;

        @NotBlank(message = "Temporary password is required")
        @Pattern(regexp = STRONG_PASSWORD, message = PASSWORD_MESSAGE)
        private String temporaryPassword;

        private UserStatus status = UserStatus.ACTIVE;
    }

    @Data
    public static class Update {
        @NotBlank
        @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
        private String fullName;

        @NotBlank
        @Pattern(regexp = USERNAME, message = "Username must be 4-30 letters, digits, dot or underscore")
        private String username;

        @NotBlank
        @Email(message = "Enter a valid email")
        private String email;

        @NotBlank
        @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
        private String phone;

        @NotNull(message = "Role is required")
        private RoleName role;

        private Long warehouseId;

        @NotNull(message = "Status is required")
        private UserStatus status;
    }

    @Data
    public static class Status {
        @NotNull(message = "Status is required")
        private UserStatus status;
    }

    @Data
    public static class RolePermissions {
        @Size(max = 255)
        private String description;

        @NotNull
        private List<Long> permissionIds;
    }

    @Data
    public static class Profile {
        @NotBlank
        @Pattern(regexp = NAME, message = "Full name must be 3-100 letters and spaces")
        private String fullName;

        @NotBlank
        @Pattern(regexp = PHONE, message = "Phone must be 10 digits")
        private String phone;

        private String profilePhotoUrl;
    }
}
