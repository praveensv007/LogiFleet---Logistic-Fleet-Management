package com.logistics.service;

import com.logistics.dto.request.MasterDataRequests;
import com.logistics.dto.request.WarehouseOpsRequests;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.dto.response.WarehouseResponse;
import com.logistics.entity.enums.ActiveStatus;

import java.util.List;

/** Warehouse master data (admin) and the warehouse floor operations (staff). */
public interface WarehouseService {

    List<WarehouseResponse> list(ActiveStatus status);

    WarehouseResponse get(Long id);

    WarehouseResponse create(MasterDataRequests.Warehouse request);

    WarehouseResponse update(Long id, MasterDataRequests.Warehouse request);

    // ----- operations -----

    ShipmentResponse receive(WarehouseOpsRequests.Receive request);

    ShipmentResponse pack(WarehouseOpsRequests.Pack request);

    ShipmentResponse markReady(WarehouseOpsRequests.Ready request);

    ShipmentResponse loadOrUnload(WarehouseOpsRequests.Loading request);

    ShipmentResponse dispatch(WarehouseOpsRequests.Dispatch request);

    /** Shipments of this warehouse still waiting for work (CREATED .. ASSIGNED). */
    List<ShipmentResponse> queue(Long warehouseId);

    List<ShipmentResponse.WarehouseTxnResponse> transactions(Long shipmentId);

    ShipmentResponse findByTracking(String trackingNumber);
}
