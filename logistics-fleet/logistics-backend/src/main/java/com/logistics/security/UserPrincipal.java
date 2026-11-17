package com.logistics.security;

import com.logistics.entity.User;
import com.logistics.entity.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Authenticated user: authorities = ROLE_xxx plus every permission code of the role. */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final String role;
    private final Long warehouseId;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getPrimaryRole();
        this.warehouseId = user.getWarehouse() != null ? user.getWarehouse().getId() : null;
        this.enabled = user.getStatus() != UserStatus.INACTIVE;
        this.accountNonLocked = user.getStatus() != UserStatus.LOCKED
                || (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now()));
        List<GrantedAuthority> list = new ArrayList<>();
        user.getRoles().forEach(r -> {
            list.add(new SimpleGrantedAuthority(r.getName()));
            r.getPermissions().forEach(p -> list.add(new SimpleGrantedAuthority(p.getCode())));
        });
        this.authorities = list;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public boolean hasAuthority(String code) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(code));
    }

    public boolean hasRole(String roleName) {
        return roleName.equals(role);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
