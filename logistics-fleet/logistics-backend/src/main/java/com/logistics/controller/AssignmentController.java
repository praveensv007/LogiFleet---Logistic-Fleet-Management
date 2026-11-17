package com.logistics.controller;

import com.logistics.dto.request.AssignmentRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.enums.AssignmentStatus;
import com.logistics.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ASSIGNMENT_MANAGE')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ApiResponse<PageResponse<AssignmentResponse>> list(@RequestParam(required = false) AssignmentStatus status,
                                                              @RequestParam(required = false) Long driverId,
                                                              @RequestParam(required = false) String search,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @RequestParam(required = false) String sort) {
        return ApiResponse.ok(assignmentService.list(status, driverId, search, page, size, sort));
    }

    @GetMapping("/pending-shipments")
    public ApiResponse<List<ShipmentResponse>> pendingShipments() {
        return ApiResponse.ok(assignmentService.pendingShipments());
    }

    @GetMapping("/{id}")
    public ApiResponse<AssignmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(assignmentService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> create(@Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.ok("Assignment created, driver notified", assignmentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssignmentResponse> update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest.Update request) {
        return ApiResponse.ok("Assignment updated", assignmentService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<AssignmentResponse> cancel(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest.Reason request) {
        return ApiResponse.ok("Assignment cancelled", assignmentService.cancel(id, request.getReason()));
    }
}
