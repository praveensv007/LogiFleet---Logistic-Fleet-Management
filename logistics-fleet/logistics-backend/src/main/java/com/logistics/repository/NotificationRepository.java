package com.logistics.repository;

import com.logistics.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUser_Id(Long userId, Pageable pageable);

    Page<Notification> findByRecipientUser_IdAndReadFalse(Long userId, Pageable pageable);

    long countByRecipientUser_IdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now where n.recipientUser.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
