package com.logistics.controller;

import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.VehicleResponse;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.entity.enums.VehicleType;
import com.logistics.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_VIEW')")
    public ApiResponse<PageResponse<VehicleResponse>> list(@RequestParam(required = false) String search,
                                                           @RequestParam(required = false) VehicleStatus status,
                                                           @RequestParam(required = false) VehicleType type,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) String sort) {
        return ApiResponse.ok(vehicleService.list(search, status, type, page, size, sort));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('VEHICLE_VIEW')")
    public ApiResponse<List<VehicleResponse>> available(@RequestParam(required = false) BigDecimal minKg) {
        return ApiResponse.ok(vehicleService.available(minKg));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_VIEW')")
    public ApiResponse<VehicleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(vehicleService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VEHICLE_EDIT')")
    public ApiResponse<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ApiResponse.ok("Vehicle created", vehicleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_EDIT')")
    public ApiResponse<VehicleResponse> update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return ApiResponse.ok("Vehicle updated", vehicleService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('VEHICLE_EDIT')")
    public ApiResponse<VehicleResponse> status(@PathVariable Long id,
                                               @Valid @RequestBody MasterDataRequests.VehicleStatusChange request) {
        return ApiResponse.ok("Status updated", vehicleService.changeStatus(id, request.getStatus()));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('VEHICLE_VIEW')")
    public ApiResponse<List<VehicleResponse.VehicleDocumentResponse>> documents(@PathVariable Long id) {
        return ApiResponse.ok(vehicleService.documents(id));
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VEHICLE_EDIT')")
    public ApiResponse<VehicleResponse.VehicleDocumentResponse> addDocument(
            @PathVariable Long id, @Valid @RequestBody MasterDataRequests.VehicleDocument request) {
        return ApiResponse.ok("Document added", vehicleService.addDocument(id, request));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('VEHICLE_EDIT')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        vehicleService.deleteDocument(id, documentId);
        return ApiResponse.ok("Document removed", null);
    }
}
