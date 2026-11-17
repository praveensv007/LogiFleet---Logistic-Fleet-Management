package com.logistics.entity;

import com.logistics.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** copied at log time */
    @Column(length = 50)
    private String username;

    @Column(length = 30)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuditAction action;

    @Column(length = 30)
    private String module;

    @Column(length = 50)
    private String entityName;

    private Long entityId;

    @Column(columnDefinition = "JSON")
    private String oldValue;

    @Column(columnDefinition = "JSON")
    private String newValue;

    @Column(length = 45)
    private String ipAddress;

    private String userAgent;
}
