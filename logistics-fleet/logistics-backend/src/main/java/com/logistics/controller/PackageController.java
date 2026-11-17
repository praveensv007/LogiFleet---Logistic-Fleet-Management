package com.logistics.controller;

import com.logistics.dto.request.PackageRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.ShipmentResponse.PackageResponse;
import com.logistics.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Customer (own shipment, before RECEIVED) or warehouse staff; checked in PackageServiceImpl. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping("/shipments/{shipmentId}/packages")
    public ApiResponse<List<PackageResponse>> list(@PathVariable Long shipmentId) {
        return ApiResponse.ok(packageService.list(shipmentId));
    }

    @PostMapping("/shipments/{shipmentId}/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageResponse> add(@PathVariable Long shipmentId, @Valid @RequestBody PackageRequest request) {
        return ApiResponse.ok("Package added", packageService.add(shipmentId, request));
    }

    @PutMapping("/packages/{id}")
    public ApiResponse<PackageResponse> update(@PathVariable Long id, @Valid @RequestBody PackageRequest request) {
        return ApiResponse.ok("Package updated", packageService.update(id, request));
    }

    @PatchMapping("/packages/{id}/status")
    public ApiResponse<PackageResponse> status(@PathVariable Long id,
                                               @Valid @RequestBody ShipmentUpdateRequest.PackageStatusChange request) {
        return ApiResponse.ok("Package status updated",
                packageService.changeStatus(id, request.getStatus(), request.getRemarks()));
    }

    @DeleteMapping("/packages/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return ApiResponse.ok("Package removed", null);
    }
}
