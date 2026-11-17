package com.logistics.controller;

import com.logistics.dto.request.CustomerUpdateRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.CustomerType;
import com.logistics.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin / staff with CUSTOMER_VIEW see everyone; a customer may read and edit their own record. */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ApiResponse<PageResponse<CustomerResponse>> list(@RequestParam(required = false) String search,
                                                            @RequestParam(required = false) CustomerType type,
                                                            @RequestParam(required = false) ActiveStatus status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(customerService.list(search, type, status, page, size, sort));
    }

    @GetMapping("/me")
    public ApiResponse<CustomerResponse> me() {
        return ApiResponse.ok(customerService.me());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return ApiResponse.ok("Customer updated", customerService.update(id, request));
    }

    @GetMapping("/{id}/shipments")
    public ApiResponse<List<ShipmentResponse>> shipments(@PathVariable Long id) {
        return ApiResponse.ok(customerService.shipments(id));
    }
}
