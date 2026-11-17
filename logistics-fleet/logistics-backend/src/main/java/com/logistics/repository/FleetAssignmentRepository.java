package com.logistics.repository;

import com.logistics.entity.FleetAssignment;
import com.logistics.entity.enums.AssignmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FleetAssignmentRepository extends JpaRepository<FleetAssignment, Long>, JpaSpecificationExecutor<FleetAssignment> {

    boolean existsByShipment_IdAndStatusIn(Long shipmentId, Collection<AssignmentStatus> statuses);

    Optional<FleetAssignment> findFirstByShipment_IdAndStatusInOrderByIdDesc(Long shipmentId, Collection<AssignmentStatus> statuses);

    Optional<FleetAssignment> findFirstByShipment_IdOrderByIdDesc(Long shipmentId);

    List<FleetAssignment> findByDriver_IdAndStatusInOrderByAssignedAtDesc(Long driverId, Collection<AssignmentStatus> statuses);

    List<FleetAssignment> findByDriver_IdOrderByAssignedAtDesc(Long driverId, Pageable pageable);

    List<FleetAssignment> findByVehicle_IdOrderByAssignedAtDesc(Long vehicleId, Pageable pageable);

    List<FleetAssignment> findByAssignedAtBetween(LocalDateTime from, LocalDateTime to);

    List<FleetAssignment> findByStatusIn(Collection<AssignmentStatus> statuses);
}
