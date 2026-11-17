package com.logistics.repository;

import com.logistics.entity.DeliveryProblem;
import com.logistics.entity.enums.ResolutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryProblemRepository extends JpaRepository<DeliveryProblem, Long> {

    List<DeliveryProblem> findByDelivery_IdOrderByReportedAtDesc(Long deliveryId);

    List<DeliveryProblem> findByResolutionStatusNotOrderByReportedAtDesc(ResolutionStatus status);

    List<DeliveryProblem> findAllByOrderByReportedAtDesc();

    long countByResolutionStatusNot(ResolutionStatus status);

    long countByDelivery_IdAndResolutionStatusNot(Long deliveryId, ResolutionStatus status);

    long countByDelivery_Driver_IdAndReportedAtBetween(Long driverId, LocalDateTime from, LocalDateTime to);
}
