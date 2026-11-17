package com.logistics.controller;

import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.request.PasswordRequests;
import com.logistics.dto.request.RegisterRequest;
import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.AuthResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Account created", authService.register(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody PasswordRequests.RefreshToken request) {
        return ApiResponse.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok("Logged out", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody PasswordRequests.ForgotPassword request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.ok("If the email is registered, a reset link valid for 30 minutes has been sent.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordRequests.ResetPassword request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset. You can log in now.", null);
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordRequests.ChangePassword request) {
        authService.changePassword(request);
        return ApiResponse.ok("Password changed", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UserRequests.Profile request) {
        return ApiResponse.ok("Profile updated", authService.updateProfile(request));
    }
}
