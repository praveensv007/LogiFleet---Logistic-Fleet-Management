package com.logistics.entity;

import com.logistics.entity.enums.MaintenanceStatus;
import com.logistics.entity.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_maintenance")
public class VehicleMaintenance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaintenanceType maintenanceType;

    @Column(length = 500)
    private String description;

    private LocalDate scheduledDate;
    private LocalDate completedDate;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(length = 100)
    private String serviceCenter;

    @Column(precision = 10, scale = 2)
    private BigDecimal labourCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;

    private LocalDate nextServiceDate;
    private Integer nextServiceOdometer;

    private String invoiceUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @OneToMany(mappedBy = "maintenance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaintenancePart> parts = new ArrayList<>();
}
