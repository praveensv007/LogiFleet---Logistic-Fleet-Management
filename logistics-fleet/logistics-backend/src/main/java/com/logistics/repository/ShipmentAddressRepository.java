package com.logistics.repository;

import com.logistics.entity.ShipmentAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentAddressRepository extends JpaRepository<ShipmentAddress, Long> {
    List<ShipmentAddress> findByShipment_Id(Long shipmentId);
}
