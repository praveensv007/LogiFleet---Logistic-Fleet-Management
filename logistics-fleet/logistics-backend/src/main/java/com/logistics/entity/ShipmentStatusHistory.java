package com.logistics.entity;

import com.logistics.entity.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shipment_status_history")
public class ShipmentStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ShipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ShipmentStatus toStatus;

    @Column(length = 150)
    private String location;

    @Column(length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    private LocalDateTime changedAt;
}
