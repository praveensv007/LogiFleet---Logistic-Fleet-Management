package com.logistics.controller;

import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.DriverResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.enums.DriverAvailability;
import com.logistics.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    @PreAuthorize("hasAuthority('DRIVER_VIEW')")
    public ApiResponse<PageResponse<DriverResponse>> list(@RequestParam(required = false) String search,
                                                          @RequestParam(required = false) DriverAvailability availability,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) String sort) {
        return ApiResponse.ok(driverService.list(search, availability, page, size, sort));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('DRIVER_VIEW')")
    public ApiResponse<List<DriverResponse>> available() {
        return ApiResponse.ok(driverService.available());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DRIVER_VIEW')")
    public ApiResponse<DriverResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(driverService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DRIVER_EDIT')")
    public ApiResponse<DriverResponse> create(@Valid @RequestBody DriverRequest request) {
        return ApiResponse.ok("Driver created", driverService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DRIVER_EDIT')")
    public ApiResponse<DriverResponse> update(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return ApiResponse.ok("Driver updated", driverService.update(id, request));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('DRIVER_EDIT')")
    public ApiResponse<DriverResponse> availability(@PathVariable Long id,
                                                    @Valid @RequestBody MasterDataRequests.Availability request) {
        return ApiResponse.ok("Availability updated", driverService.changeAvailability(id, request.getAvailabilityStatus()));
    }
}
