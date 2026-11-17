package com.logistics.mapper;

import com.logistics.dto.response.ShipmentResponse;
import com.logistics.dto.response.ShipmentResponse.AddressResponse;
import com.logistics.dto.response.ShipmentResponse.PackageResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.AddressType;

import static com.logistics.mapper.MapperUtils.name;

public final class ShipmentMapper {

    private ShipmentMapper() {
    }

    /**
     * @param withPackages include the package rows (detail screens)
     * @param assignment   latest assignment or null
     */
    public static ShipmentResponse toResponse(Shipment s, boolean withPackages, FleetAssignment assignment) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .customerId(s.getCustomer() != null ? s.getCustomer().getId() : null)
                .customerName(s.getCustomer() != null ? s.getCustomer().getFullName() : null)
                .customerCode(s.getCustomer() != null ? s.getCustomer().getCustomerCode() : null)
                .originWarehouseId(s.getOriginWarehouse() != null ? s.getOriginWarehouse().getId() : null)
                .originWarehouseName(s.getOriginWarehouse() != null ? s.getOriginWarehouse().getName() : null)
                .destinationWarehouseId(s.getDestinationWarehouse() != null ? s.getDestinationWarehouse().getId() : null)
                .destinationWarehouseName(s.getDestinationWarehouse() != null ? s.getDestinationWarehouse().getName() : null)
                .serviceType(name(s.getServiceType()))
                .description(s.getDescription())
                .totalPackages(s.getTotalPackages())
                .totalWeightKg(s.getTotalWeightKg())
                .declaredValue(s.getDeclaredValue())
                .shippingCharge(s.getShippingCharge())
                .paymentMode(name(s.getPaymentMode()))
                .specialInstructions(s.getSpecialInstructions())
                .pickupDate(s.getPickupDate())
                .pickupTimeSlot(name(s.getPickupTimeSlot()))
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .currentStatus(name(s.getCurrentStatus()))
                .cancelReason(s.getCancelReason())
                .createdAt(s.getCreatedAt())
                .pickup(toAddress(s.getAddress(AddressType.PICKUP)))
                .delivery(toAddress(s.getAddress(AddressType.DELIVERY)))
                .packages(withPackages ? s.getPackages().stream().map(ShipmentMapper::toPackage).toList() : null)
                .assignment(assignment != null ? FleetMapper.toAssignment(assignment) : null)
                .build();
    }

    public static AddressResponse toAddress(ShipmentAddress a) {
        if (a == null) {
            return null;
        }
        return AddressResponse.builder()
                .id(a.getId())
                .addressType(name(a.getAddressType()))
                .contactName(a.getContactName())
                .contactPhone(a.getContactPhone())
                .contactEmail(a.getContactEmail())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .landmark(a.getLandmark())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .country(a.getCountry())
                .build();
    }

    public static PackageResponse toPackage(ShipmentPackage p) {
        return PackageResponse.builder()
                .id(p.getId())
                .shipmentId(p.getShipment().getId())
                .packageCode(p.getPackageCode())
                .description(p.getDescription())
                .packageType(name(p.getPackageType()))
                .quantity(p.getQuantity())
                .weightKg(p.getWeightKg())
                .lengthCm(p.getLengthCm())
                .widthCm(p.getWidthCm())
                .heightCm(p.getHeightCm())
                .volumetricWeightKg(p.getVolumetricWeightKg())
                .fragile(p.isFragile())
                .packingType(name(p.getPackingType()))
                .currentStatus(name(p.getCurrentStatus()))
                .build();
    }

    public static ShipmentResponse.StatusHistoryResponse toHistory(ShipmentStatusHistory h) {
        return new ShipmentResponse.StatusHistoryResponse(h.getId(), name(h.getFromStatus()), name(h.getToStatus()),
                h.getChangedAt(), h.getLocation(), h.getRemarks(),
                h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System");
    }

    public static ShipmentResponse.WarehouseTxnResponse toTxn(WarehouseTransaction t) {
        return new ShipmentResponse.WarehouseTxnResponse(t.getId(), name(t.getTransactionType()), t.getPackageCount(),
                name(t.getConditionStatus()), t.getStorageLocation(), t.getGatePassNumber(),
                t.getShipmentPackage() != null ? t.getShipmentPackage().getPackageCode() : null,
                t.getPerformedBy() != null ? t.getPerformedBy().getFullName() : null,
                t.getTransactionTime(), t.getRemarks());
    }
}
