package com.logistics.service;

import com.logistics.dto.request.PackageRequest;
import com.logistics.dto.response.ShipmentResponse.PackageResponse;
import com.logistics.entity.enums.PackageStatus;

import java.util.List;

public interface PackageService {

    List<PackageResponse> list(Long shipmentId);

    /** Customer before RECEIVED, or warehouse staff. */
    PackageResponse add(Long shipmentId, PackageRequest request);

    PackageResponse update(Long packageId, PackageRequest request);

    PackageResponse changeStatus(Long packageId, PackageStatus status, String remarks);

    void delete(Long packageId);
}
