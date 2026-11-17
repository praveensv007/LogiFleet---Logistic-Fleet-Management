package com.logistics.repository;

import com.logistics.entity.WarehouseTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseTransactionRepository extends JpaRepository<WarehouseTransaction, Long> {

    List<WarehouseTransaction> findByShipment_IdOrderByTransactionTimeAsc(Long shipmentId);

    List<WarehouseTransaction> findByWarehouse_IdOrderByTransactionTimeDesc(Long warehouseId, Pageable pageable);
}
