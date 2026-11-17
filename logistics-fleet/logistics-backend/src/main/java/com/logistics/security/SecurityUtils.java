package com.logistics.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static UserPrincipal current() {
        return currentPrincipal().orElseThrow(
                () -> new com.logistics.exception.UnauthorizedException("Authentication required"));
    }

    public static Long currentUserId() {
        return currentPrincipal().map(UserPrincipal::getId).orElse(null);
    }

    public static boolean hasAuthority(String code) {
        return currentPrincipal().map(p -> p.hasAuthority(code)).orElse(false);
    }

    public static boolean isRole(String role) {
        return currentPrincipal().map(p -> p.hasRole(role)).orElse(false);
    }
}
