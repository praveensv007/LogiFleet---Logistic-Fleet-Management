package com.logistics.controller;

import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.WarehouseResponse;
import com.logistics.entity.enums.ActiveStatus;
import com.logistics.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Any logged-in user can read warehouses (customers pick an origin); only admins edit them. */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ApiResponse<List<WarehouseResponse>> list(@RequestParam(required = false) ActiveStatus status) {
        return ApiResponse.ok(warehouseService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(warehouseService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('WAREHOUSE_EDIT')")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody MasterDataRequests.Warehouse request) {
        return ApiResponse.ok("Warehouse created", warehouseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_EDIT')")
    public ApiResponse<WarehouseResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDataRequests.Warehouse request) {
        return ApiResponse.ok("Warehouse updated", warehouseService.update(id, request));
    }
}
