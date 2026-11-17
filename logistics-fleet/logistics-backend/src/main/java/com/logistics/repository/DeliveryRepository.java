package com.logistics.repository;

import com.logistics.entity.Delivery;
import com.logistics.entity.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByAssignment_Id(Long assignmentId);

    Optional<Delivery> findFirstByShipment_IdOrderByIdDesc(Long shipmentId);

    @Query("select d from Delivery d where d.assignment.status in :statuses order by d.id desc")
    List<Delivery> findByAssignmentStatusIn(@Param("statuses") Collection<AssignmentStatus> statuses);

    List<Delivery> findByDeliveredTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("select count(d) from Delivery d where d.delayed = true and d.assignment.status in :statuses")
    long countDelayedActive(@Param("statuses") Collection<AssignmentStatus> statuses);
}
