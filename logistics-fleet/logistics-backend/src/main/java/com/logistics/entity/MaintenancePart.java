package com.logistics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maintenance_parts")
public class MaintenancePart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_id")
    private VehicleMaintenance maintenance;

    @Column(length = 100)
    private String partName;

    @Column(length = 50)
    private String partNumber;

    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;
}
