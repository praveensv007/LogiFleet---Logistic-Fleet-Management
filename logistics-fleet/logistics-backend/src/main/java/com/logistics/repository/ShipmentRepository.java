package com.logistics.repository;

import com.logistics.entity.Shipment;
import com.logistics.entity.enums.ShipmentStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    long countByTrackingNumberStartingWith(String prefix);

    List<Shipment> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    long countByCurrentStatus(ShipmentStatus status);

    long countByCurrentStatusIn(Collection<ShipmentStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCustomer_Id(Long customerId);

    long countByCustomer_IdAndCurrentStatusIn(Long customerId, Collection<ShipmentStatus> statuses);

    List<Shipment> findByCurrentStatusAndCreatedAtBefore(ShipmentStatus status, LocalDateTime before);

    List<Shipment> findByOriginWarehouse_IdAndCurrentStatusIn(Long warehouseId, Collection<ShipmentStatus> statuses, Sort sort);

    List<Shipment> findByCurrentStatusIn(Collection<ShipmentStatus> statuses, Sort sort);

    List<Shipment> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByOriginWarehouse_IdAndCurrentStatusIn(Long warehouseId, Collection<ShipmentStatus> statuses);
}
