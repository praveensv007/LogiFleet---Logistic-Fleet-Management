package com.logistics.mapper;

import com.logistics.dto.response.AssignmentResponse;
import com.logistics.dto.response.DeliveryResponse;
import com.logistics.entity.*;
import com.logistics.entity.enums.AddressType;
import com.logistics.entity.enums.PaymentMode;

import java.math.BigDecimal;

import static com.logistics.mapper.MapperUtils.name;

public final class FleetMapper {

    private FleetMapper() {
    }

    public static BigDecimal codDue(Shipment s) {
        return s.getPaymentMode() == PaymentMode.COD ? s.getShippingCharge() : BigDecimal.ZERO;
    }

    public static AssignmentResponse toAssignment(FleetAssignment a) {
        Shipment s = a.getShipment();
        ShipmentAddress pickup = s.getAddress(AddressType.PICKUP);
        ShipmentAddress drop = s.getAddress(AddressType.DELIVERY);
        Delivery d = a.getDelivery();
        return AssignmentResponse.builder()
                .id(a.getId())
                .assignmentCode(a.getAssignmentCode())
                .shipmentId(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .shipmentStatus(name(s.getCurrentStatus()))
                .serviceType(name(s.getServiceType()))
                .paymentMode(name(s.getPaymentMode()))
                .totalPackages(s.getTotalPackages())
                .totalWeightKg(s.getTotalWeightKg())
                .codAmountDue(codDue(s))
                .pickupCity(pickup != null ? pickup.getCity() : null)
                .deliveryCity(drop != null ? drop.getCity() : null)
                .deliveryAddress(MapperUtils.oneLine(drop))
                .receiverName(drop != null ? drop.getContactName() : null)
                .receiverPhone(drop != null ? drop.getContactPhone() : null)
                .originWarehouseId(s.getOriginWarehouse() != null ? s.getOriginWarehouse().getId() : null)
                .originWarehouseName(s.getOriginWarehouse() != null ? s.getOriginWarehouse().getName() : null)
                .driverId(a.getDriver().getId())
                .driverName(a.getDriver().getFullName())
                .driverPhone(a.getDriver().getPhone())
                .vehicleId(a.getVehicle().getId())
                .vehicleNumber(a.getVehicle().getVehicleNumber())
                .vehicleType(name(a.getVehicle().getVehicleType()))
                .assignedByName(a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : null)
                .assignedAt(a.getAssignedAt())
                .plannedStartTime(a.getPlannedStartTime())
                .expectedDeliveryTime(a.getExpectedDeliveryTime())
                .routeNotes(a.getRouteNotes())
                .status(name(a.getStatus()))
                .rejectionReason(a.getRejectionReason())
                .acceptedAt(a.getAcceptedAt())
                .completedAt(a.getCompletedAt())
                .deliveryId(d != null ? d.getId() : null)
                .deliveryStatus(d != null ? name(d.getCurrentStatus()) : null)
                .delayed(d != null && d.isDelayed())
                .delayMinutes(d != null ? d.getDelayMinutes() : null)
                .build();
    }

    /** Summary row; the service adds history / problems for the detail view. */
    public static DeliveryResponse toDelivery(Delivery d, long openProblems) {
        FleetAssignment a = d.getAssignment();
        Shipment s = d.getShipment();
        return DeliveryResponse.builder()
                .id(d.getId())
                .assignmentId(a.getId())
                .assignmentCode(a.getAssignmentCode())
                .assignmentStatus(name(a.getStatus()))
                .shipmentId(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .shipmentStatus(name(s.getCurrentStatus()))
                .totalPackages(s.getTotalPackages())
                .driverId(d.getDriver().getId())
                .driverName(d.getDriver().getFullName())
                .driverPhone(d.getDriver().getPhone())
                .vehicleNumber(a.getVehicle().getVehicleNumber())
                .currentStatus(name(d.getCurrentStatus()))
                .pickupTime(d.getPickupTime())
                .deliveredTime(d.getDeliveredTime())
                .pickupPhotoUrl(d.getPickupPhotoUrl())
                .receiverName(d.getReceiverName())
                .receiverPhone(d.getReceiverPhone())
                .otpVerified(d.isOtpVerified())
                .otpSent(d.getDeliveryOtpHash() != null)
                .signatureUrl(d.getSignatureUrl())
                .podPhotoUrl(d.getPodPhotoUrl())
                .paymentMode(name(s.getPaymentMode()))
                .codAmountDue(codDue(s))
                .codAmountCollected(d.getCodAmountCollected())
                .currentLatitude(d.getCurrentLatitude())
                .currentLongitude(d.getCurrentLongitude())
                .lastLocationUpdate(d.getLastLocationUpdate())
                .delayed(d.isDelayed())
                .delayMinutes(d.getDelayMinutes())
                .expectedDeliveryTime(a.getExpectedDeliveryTime())
                .pickupAddress(ShipmentMapper.toAddress(s.getAddress(AddressType.PICKUP)))
                .deliveryAddress(ShipmentMapper.toAddress(s.getAddress(AddressType.DELIVERY)))
                .openProblems(openProblems)
                .build();
    }

    public static DeliveryResponse.HistoryItem toHistory(DeliveryStatusHistory h) {
        return new DeliveryResponse.HistoryItem(name(h.getStatus()), h.getLatitude(), h.getLongitude(),
                h.getLocationText(), h.getRemarks(), h.getCreatedAt());
    }

    public static DeliveryResponse.ProblemResponse toProblem(DeliveryProblem p) {
        Delivery d = p.getDelivery();
        return DeliveryResponse.ProblemResponse.builder()
                .id(p.getId())
                .deliveryId(d.getId())
                .trackingNumber(d.getShipment().getTrackingNumber())
                .driverName(d.getDriver().getFullName())
                .vehicleNumber(d.getAssignment().getVehicle().getVehicleNumber())
                .problemType(name(p.getProblemType()))
                .description(p.getDescription())
                .photoUrl(p.getPhotoUrl())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .reportedAt(p.getReportedAt())
                .resolutionStatus(name(p.getResolutionStatus()))
                .resolutionNotes(p.getResolutionNotes())
                .resolvedByName(p.getResolvedBy() != null ? p.getResolvedBy().getFullName() : null)
                .resolvedAt(p.getResolvedAt())
                .build();
    }
}
