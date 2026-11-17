package com.logistics.scheduler;

import com.logistics.entity.Vehicle;
import com.logistics.entity.VehicleMaintenance;
import com.logistics.entity.enums.MaintenanceStatus;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.VehicleStatus;
import com.logistics.repository.VehicleMaintenanceRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Daily 08:00: maintenance scheduled or next service due within 7 days. */
@Component
@RequiredArgsConstructor
public class MaintenanceDueJob {

    private final VehicleMaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void run() {
        LocalDate limit = LocalDate.now().plusDays(7);
        Set<Long> notified = new HashSet<>();
        for (VehicleMaintenance m : maintenanceRepository.findByStatusInAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
                EnumSet.of(MaintenanceStatus.SCHEDULED), limit)) {
            notified.add(m.getVehicle().getId());
            boolean overdue = m.getScheduledDate().isBefore(LocalDate.now());
            notificationService.notifyRole(RoleName.FLEET_MANAGER,
                    (overdue ? "Overdue: " : "Due soon: ") + m.getVehicle().getVehicleNumber(),
                    m.getMaintenanceType() + " scheduled " + m.getScheduledDate() + " - " + m.getDescription(),
                    NotificationType.MAINTENANCE_DUE, ReferenceType.MAINTENANCE, m.getId());
        }
        for (Vehicle v : vehicleRepository.findByNextServiceDateLessThanEqual(limit)) {
            if (notified.contains(v.getId()) || v.getStatus() == VehicleStatus.INACTIVE) {
                continue;
            }
            notificationService.notifyRole(RoleName.FLEET_MANAGER, "Service due: " + v.getVehicleNumber(),
                    "Next service date " + v.getNextServiceDate() + ". Schedule maintenance.",
                    NotificationType.MAINTENANCE_DUE, ReferenceType.VEHICLE, v.getId());
        }
    }
}
