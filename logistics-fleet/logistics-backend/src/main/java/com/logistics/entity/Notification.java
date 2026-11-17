package com.logistics.entity;

import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_user_id"))
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @Column(length = 150)
    private String title;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReferenceType referenceType;

    private Long referenceId;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime readAt;
}
