package com.logistics.repository;

import com.logistics.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicle_IdOrderByIdDesc(Long vehicleId);

    List<VehicleDocument> findByExpiryDateBetween(LocalDate from, LocalDate to);
}
