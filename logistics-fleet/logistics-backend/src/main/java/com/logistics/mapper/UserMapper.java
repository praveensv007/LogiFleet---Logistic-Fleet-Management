package com.logistics.mapper;

import com.logistics.dto.response.AuditLogResponse;
import com.logistics.dto.response.NotificationResponse;
import com.logistics.dto.response.RoleResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.*;

import java.util.Comparator;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getPrimaryRole())
                .status(MapperUtils.name(u.getStatus()))
                .warehouseId(u.getWarehouse() != null ? u.getWarehouse().getId() : null)
                .warehouseName(u.getWarehouse() != null ? u.getWarehouse().getName() : null)
                .profilePhotoUrl(u.getProfilePhotoUrl())
                .lastLoginAt(u.getLastLoginAt())
                .lockedUntil(u.getLockedUntil())
                .mustChangePassword(u.isMustChangePassword())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public static RoleResponse toRole(Role r) {
        return new RoleResponse(r.getId(), r.getName(), r.getDescription(),
                r.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                        .map(UserMapper::toPermission).toList());
    }

    public static RoleResponse.PermissionResponse toPermission(Permission p) {
        return new RoleResponse.PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getModule());
    }

    public static NotificationResponse toNotification(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), MapperUtils.name(n.getType()),
                MapperUtils.name(n.getReferenceType()), n.getReferenceId(), n.isRead(), n.getReadAt(), n.getCreatedAt());
    }

    public static AuditLogResponse toAudit(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getCreatedAt(), a.getUser() != null ? a.getUser().getId() : null,
                a.getUsername(), a.getRole(), MapperUtils.name(a.getAction()), a.getModule(), a.getEntityName(),
                a.getEntityId(), a.getOldValue(), a.getNewValue(), a.getIpAddress(), a.getUserAgent());
    }
}
