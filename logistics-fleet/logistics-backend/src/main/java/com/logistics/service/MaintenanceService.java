package com.logistics.service;

import com.logistics.dto.request.MaintenanceRequest;
import com.logistics.dto.response.MaintenanceResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.enums.MaintenanceStatus;

import java.util.List;

public interface MaintenanceService {

    PageResponse<MaintenanceResponse> list(Long vehicleId, MaintenanceStatus status, int page, int size, String sort);

    /** Scheduled within the next 7 days or overdue. */
    List<MaintenanceResponse> due();

    MaintenanceResponse get(Long id);

    MaintenanceResponse create(MaintenanceRequest request);

    MaintenanceResponse update(Long id, MaintenanceRequest request);

    MaintenanceResponse complete(Long id, MaintenanceRequest.Complete request);
}
