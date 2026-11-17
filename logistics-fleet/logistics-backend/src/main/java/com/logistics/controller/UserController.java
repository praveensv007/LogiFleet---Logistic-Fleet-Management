package com.logistics.controller;

import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.UserStatus;
import com.logistics.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) RoleName role,
                                                        @RequestParam(required = false) UserStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String sort) {
        return ApiResponse.ok(userService.list(search, role, status, page, size, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequests.Create request) {
        return ApiResponse.ok("User created", userService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserRequests.Update request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> status(@PathVariable Long id, @Valid @RequestBody UserRequests.Status request) {
        return ApiResponse.ok("Status updated", userService.changeStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<UserResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok("Temporary password generated", userService.resetPassword(id));
    }
}
