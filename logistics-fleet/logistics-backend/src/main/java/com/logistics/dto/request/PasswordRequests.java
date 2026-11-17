package com.logistics.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.logistics.dto.request.ValidationPatterns.PASSWORD_MESSAGE;
import static com.logistics.dto.request.ValidationPatterns.STRONG_PASSWORD;

/** Small request bodies for the auth / password screens. */
public final class PasswordRequests {

    private PasswordRequests() {
    }

    @Data
    public static class RefreshToken {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ForgotPassword {
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        private String email;
    }

    @Data
    public static class ResetPassword {
        @NotBlank(message = "Reset token is missing")
        private String token;

        @NotBlank
        @Pattern(regexp = STRONG_PASSWORD, message = PASSWORD_MESSAGE)
        private String newPassword;

        @NotBlank(message = "Confirm the new password")
        private String confirmPassword;
    }

    @Data
    public static class ChangePassword {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank
        @Pattern(regexp = STRONG_PASSWORD, message = PASSWORD_MESSAGE)
        private String newPassword;

        @NotBlank(message = "Confirm the new password")
        private String confirmPassword;
    }
}
