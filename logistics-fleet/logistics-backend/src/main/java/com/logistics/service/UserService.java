package com.logistics.service;

import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.RoleResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.UserStatus;

import java.util.List;

/** Admin user management plus roles & permissions. */
public interface UserService {

    PageResponse<UserResponse> list(String search, RoleName role, UserStatus status, int page, int size, String sort);

    UserResponse get(Long id);

    UserResponse create(UserRequests.Create request);

    UserResponse update(Long id, UserRequests.Update request);

    UserResponse changeStatus(Long id, UserStatus status);

    /** Generates a temporary password, forces change on next login and returns it once. */
    UserResponse resetPassword(Long id);

    List<RoleResponse> roles();

    List<RoleResponse.PermissionResponse> permissions();

    RoleResponse updateRolePermissions(Long roleId, UserRequests.RolePermissions request);
}
