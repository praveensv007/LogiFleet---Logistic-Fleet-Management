package com.logistics.entity;

import com.logistics.entity.enums.ProblemType;
import com.logistics.entity.enums.ResolutionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "delivery_problems")
public class DeliveryProblem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ProblemType problemType;

    @Column(length = 500)
    private String description;

    private String photoUrl;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResolutionStatus resolutionStatus = ResolutionStatus.OPEN;

    @Column(length = 500)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    private LocalDateTime resolvedAt;
}
