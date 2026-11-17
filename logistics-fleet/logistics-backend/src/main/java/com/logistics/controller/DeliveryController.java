package com.logistics.controller;

import com.logistics.dto.request.DeliveryStatusRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.DeliveryResponse;
import com.logistics.entity.enums.ResolutionStatus;
import com.logistics.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ---------- driver ----------

    @GetMapping("/deliveries/my")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<List<AssignmentResponse>> my() {
        return ApiResponse.ok(deliveryService.myAssignments());
    }

    @PatchMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<AssignmentResponse> accept(@PathVariable Long id) {
        return ApiResponse.ok("Trip accepted", deliveryService.accept(id));
    }

    @PatchMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<AssignmentResponse> reject(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest.Reason request) {
        return ApiResponse.ok("Trip rejected", deliveryService.reject(id, request.getReason()));
    }

    @GetMapping("/deliveries/{id}")
    public ApiResponse<DeliveryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(deliveryService.get(id));
    }

    @PatchMapping("/deliveries/{id}/status")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<DeliveryResponse> status(@PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest request) {
        return ApiResponse.ok("Status updated", deliveryService.updateStatus(id, request));
    }

    @PostMapping("/deliveries/{id}/location")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<DeliveryResponse> location(@PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest.Location request) {
        return ApiResponse.ok("Location shared", deliveryService.updateLocation(id, request));
    }

    @PostMapping("/deliveries/{id}/send-otp")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<DeliveryResponse.OtpResponse> sendOtp(@PathVariable Long id) {
        return ApiResponse.ok("OTP sent to the receiver", deliveryService.sendOtp(id));
    }

    @PostMapping("/deliveries/{id}/complete")
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<DeliveryResponse> complete(@PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest.Complete request) {
        return ApiResponse.ok("Delivery completed", deliveryService.complete(id, request));
    }

    @PostMapping("/deliveries/{id}/problems")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DELIVERY_UPDATE')")
    public ApiResponse<DeliveryResponse.ProblemResponse> problem(@PathVariable Long id,
                                                                 @Valid @RequestBody DeliveryStatusRequest.Problem request) {
        return ApiResponse.ok("Problem reported, fleet manager alerted", deliveryService.reportProblem(id, request));
    }

    // ---------- fleet manager / admin ----------

    @GetMapping("/deliveries/active")
    @PreAuthorize("hasAuthority('DELIVERY_MONITOR')")
    public ApiResponse<List<DeliveryResponse>> active() {
        return ApiResponse.ok(deliveryService.active());
    }

    @GetMapping("/problems")
    @PreAuthorize("hasAuthority('DELIVERY_MONITOR')")
    public ApiResponse<List<DeliveryResponse.ProblemResponse>> problems(@RequestParam(required = false) ResolutionStatus status) {
        return ApiResponse.ok(deliveryService.problems(status));
    }

    @PatchMapping("/problems/{id}/resolve")
    @PreAuthorize("hasAuthority('DELIVERY_MONITOR')")
    public ApiResponse<DeliveryResponse.ProblemResponse> resolve(@PathVariable Long id,
                                                                 @Valid @RequestBody DeliveryStatusRequest.Resolve request) {
        return ApiResponse.ok("Problem updated", deliveryService.resolveProblem(id, request));
    }
}
