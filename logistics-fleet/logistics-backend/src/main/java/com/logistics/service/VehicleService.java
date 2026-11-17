package com.logistics.service;

import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.request.VehicleRequest;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.VehicleResponse;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.entity.enums.VehicleType;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleService {

    PageResponse<VehicleResponse> list(String search, VehicleStatus status, VehicleType type, int page, int size, String sort);

    /** AVAILABLE, capacity >= minKg, insurance valid. */
    List<VehicleResponse> available(BigDecimal minKg);

    VehicleResponse get(Long id);

    VehicleResponse create(VehicleRequest request);

    VehicleResponse update(Long id, VehicleRequest request);

    VehicleResponse changeStatus(Long id, VehicleStatus status);

    List<VehicleResponse.VehicleDocumentResponse> documents(Long vehicleId);

    VehicleResponse.VehicleDocumentResponse addDocument(Long vehicleId, MasterDataRequests.VehicleDocument request);

    void deleteDocument(Long vehicleId, Long documentId);
}
