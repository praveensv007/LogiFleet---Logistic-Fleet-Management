package com.logistics.entity.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Shipment lifecycle. This spec assigns before dispatch because loading needs an assigned vehicle:
 * CREATED -> RECEIVED -> PACKED -> READY_FOR_DISPATCH -> ASSIGNED -> DISPATCHED
 *         -> PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED.
 * Cancelling is allowed only before PICKED_UP.
 */
public enum ShipmentStatus {
    CREATED,
    RECEIVED,
    PACKED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> NEXT = Map.ofEntries(
            Map.entry(CREATED, EnumSet.of(RECEIVED, CANCELLED)),
            Map.entry(RECEIVED, EnumSet.of(PACKED, CANCELLED)),
            Map.entry(PACKED, EnumSet.of(READY_FOR_DISPATCH, CANCELLED)),
            Map.entry(READY_FOR_DISPATCH, EnumSet.of(ASSIGNED, CANCELLED)),
            Map.entry(ASSIGNED, EnumSet.of(DISPATCHED, READY_FOR_DISPATCH, CANCELLED)),
            Map.entry(DISPATCHED, EnumSet.of(PICKED_UP, READY_FOR_DISPATCH, CANCELLED)),
            Map.entry(PICKED_UP, EnumSet.of(IN_TRANSIT)),
            Map.entry(IN_TRANSIT, EnumSet.of(OUT_FOR_DELIVERY)),
            Map.entry(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, IN_TRANSIT)),
            Map.entry(DELIVERED, EnumSet.noneOf(ShipmentStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(ShipmentStatus.class))
    );

    public boolean canTransitionTo(ShipmentStatus next) {
        return NEXT.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isCancellable() {
        return canTransitionTo(CANCELLED);
    }

    /** Statuses that still sit in a warehouse queue. */
    public static final Set<ShipmentStatus> WAREHOUSE_QUEUE =
            EnumSet.of(CREATED, RECEIVED, PACKED, READY_FOR_DISPATCH, ASSIGNED);

    /** Statuses where the shipment is on the road. */
    public static final Set<ShipmentStatus> ON_ROAD =
            EnumSet.of(DISPATCHED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY);
}
