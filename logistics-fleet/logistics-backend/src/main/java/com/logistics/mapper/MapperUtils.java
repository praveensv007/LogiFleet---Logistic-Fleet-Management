package com.logistics.mapper;

import com.logistics.entity.ShipmentAddress;

public final class MapperUtils {

    private MapperUtils() {
    }

    public static String name(Enum<?> e) {
        return e == null ? null : e.name();
    }

    public static String oneLine(ShipmentAddress a) {
        if (a == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(a.getAddressLine1());
        if (a.getAddressLine2() != null && !a.getAddressLine2().isBlank()) {
            sb.append(", ").append(a.getAddressLine2());
        }
        if (a.getLandmark() != null && !a.getLandmark().isBlank()) {
            sb.append(", near ").append(a.getLandmark());
        }
        sb.append(", ").append(a.getCity()).append(", ").append(a.getState()).append(" - ").append(a.getPincode());
        return sb.toString();
    }
}
