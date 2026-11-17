package com.logistics.repository;

import com.logistics.entity.ShipmentStatusHistory;
import com.logistics.entity.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, Long> {

    List<ShipmentStatusHistory> findByShipment_IdOrderByChangedAtAscIdAsc(Long shipmentId);

    long countByToStatusAndChangedAtBetween(ShipmentStatus status, LocalDateTime from, LocalDateTime to);

    List<ShipmentStatusHistory> findByToStatusAndChangedAtBetween(ShipmentStatus status, LocalDateTime from, LocalDateTime to);
}
