package com.logistics.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentRequest {

    @NotNull(message = "Select a shipment")
    private Long shipmentId;

    @NotNull(message = "Select a driver")
    private Long driverId;

    @NotNull(message = "Select a vehicle")
    private Long vehicleId;

    @NotNull(message = "Planned start is required")
    private LocalDateTime plannedStartTime;

    @NotNull(message = "Expected delivery is required")
    private LocalDateTime expectedDeliveryTime;

    @Size(max = 500)
    private String routeNotes;

    /** PUT /assignments/{id}: only times and notes can change. */
    @lombok.Data
    public static class Update {
        @NotNull
        private LocalDateTime plannedStartTime;

        @NotNull
        private LocalDateTime expectedDeliveryTime;

        @Size(max = 500)
        private String routeNotes;
    }
}
