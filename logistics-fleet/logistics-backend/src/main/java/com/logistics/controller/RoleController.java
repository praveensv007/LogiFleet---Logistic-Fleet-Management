package com.logistics.controller;

import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.RoleResponse;
import com.logistics.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class RoleController {

    private final UserService userService;

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> roles() {
        return ApiResponse.ok(userService.roles());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<RoleResponse.PermissionResponse>> permissions() {
        return ApiResponse.ok(userService.permissions());
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<RoleResponse> updatePermissions(@PathVariable Long id,
                                                       @Valid @RequestBody UserRequests.RolePermissions request) {
        return ApiResponse.ok("Permissions saved", userService.updateRolePermissions(id, request));
    }
}
