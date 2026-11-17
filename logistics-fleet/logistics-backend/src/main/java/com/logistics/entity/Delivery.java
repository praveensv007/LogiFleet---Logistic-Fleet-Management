package com.logistics.entity;

import com.logistics.entity.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row per accepted assignment. current_status is null until the driver marks PICKED_UP. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deliveries")
public class Delivery extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", unique = true)
    private FleetAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeliveryStatus currentStatus;

    private LocalDateTime pickupTime;
    private LocalDateTime deliveredTime;

    private String pickupPhotoUrl;

    @Column(length = 100)
    private String receiverName;

    @Column(length = 15)
    private String receiverPhone;

    private String deliveryOtpHash;

    private LocalDateTime otpGeneratedAt;

    private boolean otpVerified;

    private String signatureUrl;
    private String podPhotoUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal codAmountCollected;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    private LocalDateTime lastLocationUpdate;

    @Column(name = "is_delayed")
    private boolean delayed;

    private Integer delayMinutes;
}
