package com.logistics.entity;

import com.logistics.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shipments", indexes = @Index(name = "idx_shipment_tracking", columnList = "tracking_number"))
public class Shipment extends BaseEntity {

    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_warehouse_id")
    private Warehouse originWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ServiceType serviceType;

    @Column(length = 500)
    private String description;

    private Integer totalPackages;

    @Column(name = "total_weight_kg", precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(precision = 12, scale = 2)
    private BigDecimal declaredValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingCharge;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PaymentMode paymentMode;

    @Column(length = 500)
    private String specialInstructions;

    private LocalDate pickupDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private PickupTimeSlot pickupTimeSlot;

    private LocalDate estimatedDeliveryDate;

    private LocalDateTime actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ShipmentStatus currentStatus = ShipmentStatus.CREATED;

    private String cancelReason;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ShipmentPackage> packages = new ArrayList<>();

    public ShipmentAddress getAddress(AddressType type) {
        return addresses.stream().filter(a -> a.getAddressType() == type).findFirst().orElse(null);
    }

    public void addAddress(ShipmentAddress address) {
        address.setShipment(this);
        addresses.add(address);
    }

    public void addPackage(ShipmentPackage p) {
        p.setShipment(this);
        packages.add(p);
    }
}
