package com.logistics.entity;

import com.logistics.entity.enums.PackageStatus;
import com.logistics.entity.enums.PackageType;
import com.logistics.entity.enums.PackingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Maps the "packages" table. Named ShipmentPackage because "Package" clashes with java.lang.Package.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "packages")
public class ShipmentPackage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    /** tracking number + "-P1", "-P2" ... */
    @Column(unique = true, length = 30)
    private String packageCode;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PackageType packageType;

    private Integer quantity = 1;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;

    /** L x W x H / 5000 */
    @Column(name = "volumetric_weight_kg", precision = 10, scale = 2)
    private BigDecimal volumetricWeightKg;

    @Column(name = "is_fragile")
    private boolean fragile;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PackingType packingType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PackageStatus currentStatus = PackageStatus.CREATED;
}
