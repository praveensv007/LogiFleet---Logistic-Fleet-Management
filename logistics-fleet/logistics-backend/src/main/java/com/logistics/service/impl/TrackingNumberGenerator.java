package com.logistics.service.impl;

import com.logistics.repository.FleetAssignmentRepository;
import com.logistics.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** LGX-20261012-00057 style tracking numbers and ASG-000123 assignment codes. */
@Component
@RequiredArgsConstructor
public class TrackingNumberGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ShipmentRepository shipmentRepository;
    private final FleetAssignmentRepository assignmentRepository;

    public synchronized String nextTrackingNumber() {
        String prefix = "LGX-" + LocalDate.now().format(DAY) + "-";
        long seq = shipmentRepository.countByTrackingNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%05d", seq);
        while (shipmentRepository.existsByTrackingNumber(candidate)) {
            seq++;
            candidate = prefix + String.format("%05d", seq);
        }
        return candidate;
    }

    public synchronized String nextAssignmentCode() {
        return "ASG-" + String.format("%06d", assignmentRepository.count() + 1);
    }
}
