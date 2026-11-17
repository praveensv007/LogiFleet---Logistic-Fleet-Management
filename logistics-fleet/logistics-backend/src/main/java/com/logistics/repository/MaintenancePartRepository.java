package com.logistics.repository;

import com.logistics.entity.MaintenancePart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePartRepository extends JpaRepository<MaintenancePart, Long> {
}
