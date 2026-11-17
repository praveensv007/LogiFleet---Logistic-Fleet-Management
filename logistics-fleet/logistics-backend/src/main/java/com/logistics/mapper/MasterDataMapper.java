package com.logistics.mapper;

import com.logistics.dto.response.*;
import com.logistics.entity.*;

import java.time.LocalDate;
import java.time.Period;

import static com.logistics.mapper.MapperUtils.name;

public final class MasterDataMapper {

    private MasterDataMapper() {
    }

    public static CustomerResponse toCustomer(Customer c, long totalShipments) {
        return CustomerResponse.builder()
                .id(c.getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .username(c.getUser() != null ? c.getUser().getUsername() : null)
                .customerCode(c.getCustomerCode())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .alternatePhone(c.getAlternatePhone())
                .customerType(name(c.getCustomerType()))
                .companyName(c.getCompanyName())
                .gstNumber(c.getGstNumber())
                .addressLine1(c.getAddressLine1())
                .addressLine2(c.getAddressLine2())
                .city(c.getCity())
                .state(c.getState())
                .pincode(c.getPincode())
                .country(c.getCountry())
                .status(name(c.getStatus()))
                .totalShipments(totalShipments)
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static DriverResponse toDriver(Driver d) {
        LocalDate today = LocalDate.now();
        return DriverResponse.builder()
                .id(d.getId())
                .userId(d.getUser() != null ? d.getUser().getId() : null)
                .username(d.getUser() != null ? d.getUser().getUsername() : null)
                .email(d.getUser() != null ? d.getUser().getEmail() : null)
                .userStatus(d.getUser() != null ? name(d.getUser().getStatus()) : null)
                .driverCode(d.getDriverCode())
                .fullName(d.getFullName())
                .dateOfBirth(d.getDateOfBirth())
                .age(d.getDateOfBirth() != null ? Period.between(d.getDateOfBirth(), today).getYears() : null)
                .gender(name(d.getGender()))
                .phone(d.getPhone())
                .address(d.getAddress())
                .city(d.getCity())
                .state(d.getState())
                .pincode(d.getPincode())
                .licenseNumber(d.getLicenseNumber())
                .licenseType(name(d.getLicenseType()))
                .licenseIssueDate(d.getLicenseIssueDate())
                .licenseExpiryDate(d.getLicenseExpiryDate())
                .licenseValid(d.getLicenseExpiryDate() != null && d.getLicenseExpiryDate().isAfter(today))
                .licenseDocUrl(d.getLicenseDocUrl())
                .idProofType(name(d.getIdProofType()))
                .idProofNumber(d.getIdProofNumber())
                .experienceYears(d.getExperienceYears())
                .joiningDate(d.getJoiningDate())
                .emergencyContactName(d.getEmergencyContactName())
                .emergencyContactPhone(d.getEmergencyContactPhone())
                .photoUrl(d.getPhotoUrl())
                .availabilityStatus(name(d.getAvailabilityStatus()))
                .currentLatitude(d.getCurrentLatitude())
                .currentLongitude(d.getCurrentLongitude())
                .build();
    }

    public static VehicleResponse toVehicle(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .vehicleNumber(v.getVehicleNumber())
                .vehicleType(name(v.getVehicleType()))
                .make(v.getMake())
                .model(v.getModel())
                .manufactureYear(v.getManufactureYear())
                .fuelType(name(v.getFuelType()))
                .capacityKg(v.getCapacityKg())
                .capacityCbm(v.getCapacityCbm())
                .chassisNumber(v.getChassisNumber())
                .engineNumber(v.getEngineNumber())
                .insuranceNumber(v.getInsuranceNumber())
                .insuranceExpiry(v.getInsuranceExpiry())
                .permitExpiry(v.getPermitExpiry())
                .fitnessExpiry(v.getFitnessExpiry())
                .pucExpiry(v.getPucExpiry())
                .odometerKm(v.getOdometerKm())
                .homeWarehouseId(v.getHomeWarehouse() != null ? v.getHomeWarehouse().getId() : null)
                .homeWarehouseName(v.getHomeWarehouse() != null ? v.getHomeWarehouse().getName() : null)
                .lastServiceDate(v.getLastServiceDate())
                .nextServiceDate(v.getNextServiceDate())
                .status(name(v.getStatus()))
                .insuranceValid(v.getInsuranceExpiry() != null && v.getInsuranceExpiry().isAfter(LocalDate.now()))
                .build();
    }

    public static VehicleResponse.VehicleDocumentResponse toDocument(VehicleDocument d) {
        return new VehicleResponse.VehicleDocumentResponse(d.getId(), name(d.getDocumentType()),
                d.getDocumentNumber(), d.getFileUrl(), d.getExpiryDate());
    }

    public static WarehouseResponse toWarehouse(Warehouse w, long pendingShipments) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .warehouseCode(w.getWarehouseCode())
                .name(w.getName())
                .addressLine1(w.getAddressLine1())
                .city(w.getCity())
                .state(w.getState())
                .pincode(w.getPincode())
                .country(w.getCountry())
                .latitude(w.getLatitude())
                .longitude(w.getLongitude())
                .managerUserId(w.getManager() != null ? w.getManager().getId() : null)
                .managerName(w.getManager() != null ? w.getManager().getFullName() : null)
                .contactPhone(w.getContactPhone())
                .contactEmail(w.getContactEmail())
                .capacity(w.getCapacity())
                .openingTime(w.getOpeningTime())
                .closingTime(w.getClosingTime())
                .status(name(w.getStatus()))
                .pendingShipments(pendingShipments)
                .build();
    }

    public static MaintenanceResponse toMaintenance(VehicleMaintenance m) {
        return MaintenanceResponse.builder()
                .id(m.getId())
                .vehicleId(m.getVehicle().getId())
                .vehicleNumber(m.getVehicle().getVehicleNumber())
                .maintenanceType(name(m.getMaintenanceType()))
                .description(m.getDescription())
                .scheduledDate(m.getScheduledDate())
                .completedDate(m.getCompletedDate())
                .odometerKm(m.getOdometerKm())
                .serviceCenter(m.getServiceCenter())
                .labourCost(m.getLabourCost())
                .totalCost(m.getTotalCost())
                .nextServiceDate(m.getNextServiceDate())
                .nextServiceOdometer(m.getNextServiceOdometer())
                .invoiceUrl(m.getInvoiceUrl())
                .status(name(m.getStatus()))
                .overdue(m.getStatus() == com.logistics.entity.enums.MaintenanceStatus.SCHEDULED
                        && m.getScheduledDate() != null && m.getScheduledDate().isBefore(LocalDate.now()))
                .parts(m.getParts().stream().map(p -> new MaintenanceResponse.PartResponse(p.getId(),
                        p.getPartName(), p.getPartNumber(), p.getQuantity(), p.getUnitCost(), p.getTotalCost())).toList())
                .build();
    }
}
