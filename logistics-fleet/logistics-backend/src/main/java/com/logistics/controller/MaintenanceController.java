package com.logistics.controller;

import com.logistics.dto.request.MaintenanceRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.MaintenanceResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.enums.MaintenanceStatus;
import com.logistics.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    public ApiResponse<PageResponse<MaintenanceResponse>> list(@RequestParam(required = false) Long vehicleId,
                                                               @RequestParam(required = false) MaintenanceStatus status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String sort) {
        return ApiResponse.ok(maintenanceService.list(vehicleId, status, page, size, sort));
    }

    @GetMapping("/due")
    public ApiResponse<List<MaintenanceResponse>> due() {
        return ApiResponse.ok(maintenanceService.due());
    }

    @GetMapping("/{id}")
    public ApiResponse<MaintenanceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(maintenanceService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MaintenanceResponse> create(@Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.ok("Maintenance scheduled", maintenanceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MaintenanceResponse> update(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.ok("Maintenance updated", maintenanceService.update(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<MaintenanceResponse> complete(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest.Complete request) {
        return ApiResponse.ok("Maintenance completed", maintenanceService.complete(id, request));
    }
}
