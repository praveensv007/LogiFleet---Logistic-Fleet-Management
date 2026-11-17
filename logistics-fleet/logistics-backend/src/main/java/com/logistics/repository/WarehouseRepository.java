package com.logistics.repository;

import com.logistics.entity.Warehouse;
import com.logistics.entity.enums.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByWarehouseCode(String code);

    boolean existsByWarehouseCodeAndIdNot(String code, Long id);

    List<Warehouse> findByStatusOrderByNameAsc(ActiveStatus status);

    Optional<Warehouse> findFirstByStatusAndPincode(ActiveStatus status, String pincode);

    Optional<Warehouse> findFirstByStatusAndCityIgnoreCase(ActiveStatus status, String city);

    Optional<Warehouse> findFirstByStatusAndStateIgnoreCase(ActiveStatus status, String state);

    Optional<Warehouse> findFirstByStatusOrderByIdAsc(ActiveStatus status);
}
