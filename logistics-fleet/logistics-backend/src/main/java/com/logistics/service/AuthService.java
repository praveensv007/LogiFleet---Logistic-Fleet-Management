package com.logistics.service;

import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.request.PasswordRequests;
import com.logistics.dto.request.RegisterRequest;
import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.AuthResponse;
import com.logistics.dto.response.UserResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refresh(String refreshToken);

    void logout();

    void forgotPassword(String email);

    void resetPassword(PasswordRequests.ResetPassword request);

    void changePassword(PasswordRequests.ChangePassword request);

    UserResponse me();

    UserResponse updateProfile(UserRequests.Profile request);
}
