package com.logistics.controller;

import com.logistics.dto.request.WarehouseOpsRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WAREHOUSE_OPS')")
public class WarehouseOpsController {

    private final WarehouseService warehouseService;

    @PostMapping("/receive")
    public ApiResponse<ShipmentResponse> receive(@Valid @RequestBody WarehouseOpsRequests.Receive request) {
        return ApiResponse.ok("Shipment received", warehouseService.receive(request));
    }

    @PostMapping("/pack")
    public ApiResponse<ShipmentResponse> pack(@Valid @RequestBody WarehouseOpsRequests.Pack request) {
        return ApiResponse.ok("Package packed", warehouseService.pack(request));
    }

    @PostMapping("/ready")
    public ApiResponse<ShipmentResponse> ready(@Valid @RequestBody WarehouseOpsRequests.Ready request) {
        return ApiResponse.ok("Marked ready for dispatch", warehouseService.markReady(request));
    }

    @PostMapping("/load")
    public ApiResponse<ShipmentResponse> load(@Valid @RequestBody WarehouseOpsRequests.Loading request) {
        return ApiResponse.ok("Loading recorded", warehouseService.loadOrUnload(request));
    }

    @PostMapping("/dispatch")
    public ApiResponse<ShipmentResponse> dispatch(@Valid @RequestBody WarehouseOpsRequests.Dispatch request) {
        return ApiResponse.ok("Shipment dispatched", warehouseService.dispatch(request));
    }

    @GetMapping("/{id}/queue")
    public ApiResponse<List<ShipmentResponse>> queue(@PathVariable Long id) {
        return ApiResponse.ok(warehouseService.queue(id));
    }

    @GetMapping("/lookup/{trackingNumber}")
    public ApiResponse<ShipmentResponse> lookup(@PathVariable String trackingNumber) {
        return ApiResponse.ok(warehouseService.findByTracking(trackingNumber));
    }

    @GetMapping("/shipments/{shipmentId}/transactions")
    public ApiResponse<List<ShipmentResponse.WarehouseTxnResponse>> transactions(@PathVariable Long shipmentId) {
        return ApiResponse.ok(warehouseService.transactions(shipmentId));
    }
}
