package com.logistics.controller;

import com.logistics.dto.request.ShipmentCreateRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.enums.ShipmentStatus;
import com.logistics.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Ownership (customer vs staff) is checked in ShipmentServiceImpl. */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public ApiResponse<ShipmentResponse> create(@Valid @RequestBody ShipmentCreateRequest request) {
        return ApiResponse.ok("Shipment created", shipmentService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ShipmentResponse>> list(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(shipmentService.list(status, from, to, warehouseId, search, page, size, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShipmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(shipmentService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShipmentResponse> update(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest request) {
        return ApiResponse.ok("Shipment updated", shipmentService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ShipmentResponse> cancel(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest.Reason request) {
        return ApiResponse.ok("Shipment cancelled", shipmentService.cancel(id, request.getReason()));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ShipmentResponse.StatusHistoryResponse>> history(@PathVariable Long id) {
        return ApiResponse.ok(shipmentService.history(id));
    }
}
