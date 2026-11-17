package com.logistics.service;

import com.logistics.dto.request.AssignmentRequest;
import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.enums.AssignmentStatus;

import java.util.List;

public interface AssignmentService {

    PageResponse<AssignmentResponse> list(AssignmentStatus status, Long driverId, String search, int page, int size, String sort);

    /** READY_FOR_DISPATCH shipments with no open assignment. */
    List<ShipmentResponse> pendingShipments();

    AssignmentResponse get(Long id);

    AssignmentResponse create(AssignmentRequest request);

    AssignmentResponse update(Long id, AssignmentRequest.Update request);

    AssignmentResponse cancel(Long id, String reason);
}
