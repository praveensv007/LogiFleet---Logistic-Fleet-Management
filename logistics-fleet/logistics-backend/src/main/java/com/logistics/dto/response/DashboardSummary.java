package com.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Counters for the admin / fleet dashboards, also pushed on /topic/admin/dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private long totalShipments;
    private long shipmentsToday;
    private long deliveredToday;
    private long pendingShipments;
    private long inWarehouse;
    private long onRoad;
    private long delivered;
    private long cancelled;
    private long activeDeliveries;
    private long delayedDeliveries;
    private long openProblems;
    private long availableDrivers;
    private long driversOnTrip;
    private long availableVehicles;
    private long vehiclesOnTrip;
    private long vehiclesInMaintenance;
    private long maintenanceDue;
    private long totalCustomers;
    private long totalWarehouses;
    private long totalUsers;
    private Map<String, Long> statusBreakdown;
}
