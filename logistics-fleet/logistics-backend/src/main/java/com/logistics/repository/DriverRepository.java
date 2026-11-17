package com.logistics.repository;

import com.logistics.entity.Driver;
import com.logistics.entity.enums.DriverAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {

    Optional<Driver> findByUser_Id(Long userId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    List<Driver> findByAvailabilityStatusAndLicenseExpiryDateAfterOrderByFullNameAsc(
            DriverAvailability status, LocalDate date);

    List<Driver> findByLicenseExpiryDateBetween(LocalDate from, LocalDate to);

    long countByAvailabilityStatus(DriverAvailability status);
}
