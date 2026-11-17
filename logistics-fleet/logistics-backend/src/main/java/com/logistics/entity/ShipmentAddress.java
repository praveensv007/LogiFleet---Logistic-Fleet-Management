package com.logistics.entity;

import com.logistics.entity.enums.AddressType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shipment_addresses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shipment_id", "address_type"}))
public class ShipmentAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 10)
    private AddressType addressType;

    @Column(length = 100)
    private String contactName;

    @Column(length = 15)
    private String contactPhone;

    @Column(length = 100)
    private String contactEmail;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(length = 150)
    private String landmark;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 60)
    private String country = "India";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
