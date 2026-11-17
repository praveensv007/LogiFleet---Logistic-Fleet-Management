package com.logistics.dto.response;

import java.util.List;

public record RoleResponse(Long id, String name, String description, List<PermissionResponse> permissions) {

    public record PermissionResponse(Long id, String code, String name, String module) {
    }
}
