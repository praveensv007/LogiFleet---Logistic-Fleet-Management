package com.logistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String status;
    private Long warehouseId;
    private String warehouseName;
    private String profilePhotoUrl;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lockedUntil;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
    /** filled for /auth/me only */
    private List<String> permissions;
    private Long customerId;
    private Long driverId;
    /** filled when an admin resets a password */
    private String temporaryPassword;
}
