package com.logistics.repository;

import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);

    boolean existsByChassisNumber(String chassisNumber);

    boolean existsByChassisNumberAndIdNot(String chassisNumber, Long id);

    boolean existsByEngineNumber(String engineNumber);

    boolean existsByEngineNumberAndIdNot(String engineNumber, Long id);

    List<Vehicle> findByStatusAndCapacityKgGreaterThanEqualAndInsuranceExpiryAfterOrderByCapacityKgAsc(
            VehicleStatus status, BigDecimal minKg, LocalDate today);

    long countByStatus(VehicleStatus status);

    List<Vehicle> findByNextServiceDateLessThanEqual(LocalDate date);

    @Query("select v from Vehicle v where v.status <> com.logistics.entity.enums.VehicleStatus.INACTIVE and (" +
            "v.insuranceExpiry <= :limit or v.permitExpiry <= :limit or " +
            "v.fitnessExpiry <= :limit or v.pucExpiry <= :limit)")
    List<Vehicle> findWithDocumentsExpiringBefore(@Param("limit") LocalDate limit);
}
