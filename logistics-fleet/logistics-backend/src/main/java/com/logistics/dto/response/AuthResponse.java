package com.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private List<String> permissions;
    private boolean mustChangePassword;
    private Long warehouseId;
    private Long customerId;
    private Long driverId;
    private String profilePhotoUrl;
}
