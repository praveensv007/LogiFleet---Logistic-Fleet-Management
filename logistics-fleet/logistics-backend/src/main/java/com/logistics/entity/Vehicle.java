package com.logistics.entity;

import com.logistics.entity.enums.FuelType;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 15)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    private Integer manufactureYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private FuelType fuelType;

    @Column(name = "capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "capacity_cbm", precision = 8, scale = 2)
    private BigDecimal capacityCbm;

    @Column(unique = true, length = 30)
    private String chassisNumber;

    @Column(unique = true, length = 30)
    private String engineNumber;

    @Column(length = 30)
    private String insuranceNumber;

    private LocalDate insuranceExpiry;
    private LocalDate permitExpiry;
    private LocalDate fitnessExpiry;
    private LocalDate pucExpiry;

    @Column(name = "odometer_km")
    private Integer odometerKm = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_warehouse_id")
    private Warehouse homeWarehouse;

    private LocalDate lastServiceDate;
    private LocalDate nextServiceDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VehicleStatus status = VehicleStatus.AVAILABLE;
}
