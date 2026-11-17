package com.logistics.controller;

import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.TrackingResponse;
import com.logistics.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: no login needed. */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingController {

    private final ShipmentService shipmentService;

    @GetMapping("/{trackingNumber}")
    public ApiResponse<TrackingResponse> track(@PathVariable String trackingNumber) {
        return ApiResponse.ok(shipmentService.track(trackingNumber));
    }
}
