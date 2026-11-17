package com.logistics.scheduler;

import com.logistics.entity.Driver;
import com.logistics.entity.Vehicle;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.entity.enums.RoleName;
import com.logistics.repository.DriverRepository;
import com.logistics.repository.VehicleDocumentRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Daily 08:00: driver licenses and vehicle documents expiring within 30 days. */
@Component
@RequiredArgsConstructor
public class DocumentExpiryJob {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(30);

        for (Driver d : driverRepository.findByLicenseExpiryDateBetween(today.minusDays(365), limit)) {
            String text = "License " + d.getLicenseNumber() + " of " + d.getFullName()
                    + (d.getLicenseExpiryDate().isBefore(today) ? " expired on " : " expires on ") + d.getLicenseExpiryDate();
            notificationService.notifyRole(RoleName.FLEET_MANAGER, "Driver license expiring", text,
                    NotificationType.DOCUMENT_EXPIRING, ReferenceType.DRIVER, d.getId());
            if (d.getUser() != null) {
                notificationService.notifyUser(d.getUser(), "Renew your driving license", text,
                        NotificationType.DOCUMENT_EXPIRING, ReferenceType.DRIVER, d.getId());
            }
        }

        for (Vehicle v : vehicleRepository.findWithDocumentsExpiringBefore(limit)) {
            List<String> items = new ArrayList<>();
            add(items, "Insurance", v.getInsuranceExpiry(), limit);
            add(items, "Permit", v.getPermitExpiry(), limit);
            add(items, "Fitness", v.getFitnessExpiry(), limit);
            add(items, "PUC", v.getPucExpiry(), limit);
            notificationService.notifyRole(RoleName.FLEET_MANAGER, "Documents expiring: " + v.getVehicleNumber(),
                    String.join(", ", items), NotificationType.DOCUMENT_EXPIRING, ReferenceType.VEHICLE, v.getId());
        }

        documentRepository.findByExpiryDateBetween(today, limit).forEach(doc ->
                notificationService.notifyRole(RoleName.FLEET_MANAGER,
                        doc.getDocumentType() + " expiring: " + doc.getVehicle().getVehicleNumber(),
                        "Document " + (doc.getDocumentNumber() != null ? doc.getDocumentNumber() : "")
                                + " expires on " + doc.getExpiryDate(),
                        NotificationType.DOCUMENT_EXPIRING, ReferenceType.VEHICLE, doc.getVehicle().getId()));
    }

    private static void add(List<String> items, String label, LocalDate date, LocalDate limit) {
        if (date != null && !date.isAfter(limit)) {
            items.add(label + " " + date);
        }
    }
}
