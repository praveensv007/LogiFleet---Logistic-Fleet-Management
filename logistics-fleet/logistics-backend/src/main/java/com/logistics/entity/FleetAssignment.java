package com.logistics.entity;

import com.logistics.entity.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fleet_assignments")
public class FleetAssignment extends BaseEntity {

    @Column(unique = true, length = 20)
    private String assignmentCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    private LocalDateTime assignedAt;
    private LocalDateTime plannedStartTime;
    private LocalDateTime expectedDeliveryTime;

    @Column(length = 500)
    private String routeNotes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    private String rejectionReason;

    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
    private Delivery delivery;
}
