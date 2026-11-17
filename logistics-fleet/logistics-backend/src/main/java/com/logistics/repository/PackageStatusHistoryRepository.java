package com.logistics.repository;

import com.logistics.entity.PackageStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageStatusHistoryRepository extends JpaRepository<PackageStatusHistory, Long> {
    List<PackageStatusHistory> findByShipmentPackage_IdOrderByChangedAtAsc(Long packageId);
}
