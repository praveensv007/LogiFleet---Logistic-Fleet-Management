package com.logistics.service;

import com.logistics.dto.request.ShipmentCreateRequest;
import com.logistics.dto.request.ShipmentUpdateRequest;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.dto.response.TrackingResponse;
import com.logistics.entity.enums.ShipmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface ShipmentService {

    ShipmentResponse create(ShipmentCreateRequest request);

    /** Customers see their own shipments; staff with SHIPMENT_VIEW_ALL see all. */
    PageResponse<ShipmentResponse> list(ShipmentStatus status, LocalDate from, LocalDate to, Long warehouseId,
                                        String search, int page, int size, String sort);

    ShipmentResponse get(Long id);

    ShipmentResponse update(Long id, ShipmentUpdateRequest request);

    ShipmentResponse cancel(Long id, String reason);

    List<ShipmentResponse.StatusHistoryResponse> history(Long id);

    /** Public tracking page. */
    TrackingResponse track(String trackingNumber);
}
