package com.logistics.repository;

import com.logistics.entity.VehicleMaintenance;
import com.logistics.entity.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long>, JpaSpecificationExecutor<VehicleMaintenance> {

    List<VehicleMaintenance> findByVehicle_IdOrderByScheduledDateDesc(Long vehicleId);

    List<VehicleMaintenance> findByStatusInAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
            Collection<MaintenanceStatus> statuses, LocalDate date);

    List<VehicleMaintenance> findByStatusAndCompletedDateBetween(MaintenanceStatus status, LocalDate from, LocalDate to);

    long countByStatusIn(Collection<MaintenanceStatus> statuses);
}
