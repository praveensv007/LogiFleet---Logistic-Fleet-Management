package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.LoginRequest;
import com.logistics.dto.request.PasswordRequests;
import com.logistics.dto.request.RegisterRequest;
import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.AuthResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.Customer;
import com.logistics.entity.PasswordResetToken;
import com.logistics.entity.User;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.exception.UnauthorizedException;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.*;
import com.logistics.security.JwtUtil;
import com.logistics.security.SecurityUtils;
import com.logistics.service.AuditService;
import com.logistics.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional(noRollbackFor = {UnauthorizedException.class, BadRequestException.class})
    public AuthResponse login(LoginRequest request) {
        String key = request.getUsernameOrEmail().trim();
        User user = userRepository.findByUsernameOrEmail(key, key.toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("Your account is inactive. Contact the administrator.");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() == null) {
                throw new UnauthorizedException("Your account is locked. Contact the administrator.");
            }
            if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new UnauthorizedException("Too many failed attempts. Account locked until "
                        + user.getLockedUntil().format(DateTimeFormatter.ofPattern("hh:mm a")) + ".");
            }
            // lock expired: unlock automatically
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                throw new UnauthorizedException("Too many failed attempts. Account locked for " + LOCK_MINUTES + " minutes.");
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid username or password ("
                    + (MAX_FAILED_ATTEMPTS - attempts) + " attempts left)");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.logFor(user, AuditAction.LOGIN, "AUTH");
        return buildAuth(user, request.isRememberMe());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (!r.getPassword().equals(r.getConfirmPassword())) {
            throw new BadRequestException("confirmPassword", "Passwords do not match");
        }
        if (r.getCustomerType() == CustomerType.BUSINESS && (r.getCompanyName() == null || r.getCompanyName().isBlank())) {
            throw new BadRequestException("companyName", "Company name is required for business customers");
        }
        String email = r.getEmail().trim().toLowerCase();
        checkUnique(r.getUsername(), email, r.getPhone(), null);

        User user = new User();
        user.setUsername(r.getUsername().trim());
        user.setEmail(email);
        user.setPhone(r.getPhone());
        user.setFullName(r.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(r.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(Set.of(roleRepository.findByName(RoleName.CUSTOMER.authority())
                .orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.CUSTOMER)))));
        userRepository.save(user);

        Customer c = new Customer();
        c.setUser(user);
        c.setFullName(user.getFullName());
        c.setEmail(email);
        c.setPhone(r.getPhone());
        c.setCustomerType(r.getCustomerType());
        c.setCompanyName(blankToNull(r.getCompanyName()));
        c.setGstNumber(blankToNull(r.getGstNumber()));
        c.setAddressLine1(r.getAddressLine1());
        c.setAddressLine2(blankToNull(r.getAddressLine2()));
        c.setCity(r.getCity());
        c.setState(r.getState());
        c.setPincode(r.getPincode());
        c.setCountry(r.getCountry());
        customerRepository.save(c);
        c.setCustomerCode(String.format("CUS%06d", c.getId()));

        auditService.logFor(user, AuditAction.CREATE, "CUSTOMER");
        return buildAuth(user, false);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        String username = jwtUtil.validate(refreshToken, JwtUtil.TYPE_REFRESH);
        if (username == null) {
            throw new UnauthorizedException("Session expired. Please log in again.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please log in again."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account is not active.");
        }
        AuthResponse response = buildAuth(user, false);
        response.setRefreshToken(refreshToken); // keep the original expiry
        return response;
    }

    @Override
    @Transactional
    public void logout() {
        SecurityUtils.currentPrincipal().flatMap(p -> userRepository.findById(p.getId()))
                .ifPresent(u -> auditService.logFor(u, AuditAction.LOGOUT, "AUTH"));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        // Always answer the same way so the endpoint does not reveal which emails exist.
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            PasswordResetToken t = new PasswordResetToken();
            t.setUser(user);
            t.setToken(UUID.randomUUID().toString());
            t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            resetTokenRepository.save(t);
            String link = frontendUrl + "/reset-password?token=" + t.getToken();
            emailService.send(user.getEmail(), "Reset your password",
                    "Hello " + user.getFullName() + ",\n\nUse this link within 30 minutes to reset your password:\n"
                            + link + "\n\nIf you did not ask for this, ignore this email.");
        });
    }

    @Override
    @Transactional
    public void resetPassword(PasswordRequests.ResetPassword r) {
        if (!r.getNewPassword().equals(r.getConfirmPassword())) {
            throw new BadRequestException("confirmPassword", "Passwords do not match");
        }
        PasswordResetToken t = resetTokenRepository.findByToken(r.getToken())
                .orElseThrow(() -> new BadRequestException("This reset link is invalid."));
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link has expired. Request a new one.");
        }
        User user = t.getUser();
        user.setPasswordHash(passwordEncoder.encode(r.getNewPassword()));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        if (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
        }
        t.setUsed(true);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, module = "AUTH", entity = "Password")
    public void changePassword(PasswordRequests.ChangePassword r) {
        User user = currentUser();
        if (!passwordEncoder.matches(r.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("currentPassword", "Current password is incorrect");
        }
        if (!r.getNewPassword().equals(r.getConfirmPassword())) {
            throw new BadRequestException("confirmPassword", "Passwords do not match");
        }
        if (passwordEncoder.matches(r.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("newPassword", "New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(r.getNewPassword()));
        user.setMustChangePassword(false);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me() {
        User user = currentUser();
        UserResponse res = UserMapper.toResponse(user);
        res.setPermissions(permissions(user));
        customerRepository.findByUser_Id(user.getId()).ifPresent(c -> res.setCustomerId(c.getId()));
        driverRepository.findByUser_Id(user.getId()).ifPresent(d -> res.setDriverId(d.getId()));
        return res;
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, module = "PROFILE", entity = "User")
    public UserResponse updateProfile(UserRequests.Profile r) {
        User user = currentUser();
        if (userRepository.existsByPhoneAndIdNot(r.getPhone(), user.getId())) {
            throw new BadRequestException("phone", "Phone is already used by another account");
        }
        user.setFullName(r.getFullName().trim());
        user.setPhone(r.getPhone());
        user.setProfilePhotoUrl(blankToNull(r.getProfilePhotoUrl()));
        customerRepository.findByUser_Id(user.getId()).ifPresent(c -> {
            c.setFullName(user.getFullName());
            c.setPhone(user.getPhone());
        });
        driverRepository.findByUser_Id(user.getId()).ifPresent(d -> {
            d.setFullName(user.getFullName());
            d.setPhone(user.getPhone());
        });
        return me();
    }

    // ---------- helpers ----------

    private AuthResponse buildAuth(User user, boolean rememberMe) {
        String role = user.getPrimaryRole();
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), role))
                .refreshToken(jwtUtil.generateRefreshToken(user.getUsername(), role, rememberMe))
                .expiresIn(jwtUtil.getAccessExpiry() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role)
                .permissions(permissions(user))
                .mustChangePassword(user.isMustChangePassword())
                .warehouseId(user.getWarehouse() != null ? user.getWarehouse().getId() : null)
                .customerId(customerRepository.findByUser_Id(user.getId()).map(Customer::getId).orElse(null))
                .driverId(driverRepository.findByUser_Id(user.getId()).map(d -> d.getId()).orElse(null))
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .build();
    }

    private List<String> permissions(User user) {
        return user.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode()).distinct().sorted().toList();
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.current().getId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
    }

    void checkUnique(String username, String email, String phone, Long excludeId) {
        boolean usernameTaken = excludeId == null ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);
        if (usernameTaken) throw new BadRequestException("username", "Username is already taken");
        boolean emailTaken = excludeId == null ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId);
        if (emailTaken) throw new BadRequestException("email", "Email is already registered");
        boolean phoneTaken = excludeId == null ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, excludeId);
        if (phoneTaken) throw new BadRequestException("phone", "Phone is already registered");
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
