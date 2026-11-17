package com.logistics.entity;

import com.logistics.entity.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "package_status_history")
public class PackageStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id")
    private ShipmentPackage shipmentPackage;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PackageStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PackageStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    private LocalDateTime changedAt;
}
