package com.logistics.repository;

import com.logistics.entity.ShipmentPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageRepository extends JpaRepository<ShipmentPackage, Long> {
    List<ShipmentPackage> findByShipment_IdOrderByIdAsc(Long shipmentId);
}
