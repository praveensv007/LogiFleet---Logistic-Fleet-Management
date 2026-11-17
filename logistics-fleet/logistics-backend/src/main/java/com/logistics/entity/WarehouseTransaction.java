package com.logistics.entity;

import com.logistics.entity.enums.ConditionStatus;
import com.logistics.entity.enums.WarehouseTxnType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouse_transactions")
public class WarehouseTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private ShipmentPackage shipmentPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private FleetAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WarehouseTxnType transactionType;

    private Integer packageCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ConditionStatus conditionStatus;

    @Column(length = 30)
    private String storageLocation;

    @Column(length = 30)
    private String gatePassNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    private LocalDateTime transactionTime;

    @Column(length = 500)
    private String remarks;
}
