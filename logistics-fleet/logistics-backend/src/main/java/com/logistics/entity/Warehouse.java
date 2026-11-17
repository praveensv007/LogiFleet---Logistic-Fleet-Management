package com.logistics.entity;

import com.logistics.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String warehouseCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false, length = 60)
    private String country = "India";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private User manager;

    @Column(length = 15)
    private String contactPhone;

    @Column(length = 100)
    private String contactEmail;

    /** Total capacity in kg or pallets. */
    private Integer capacity;

    private LocalTime openingTime;
    private LocalTime closingTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
