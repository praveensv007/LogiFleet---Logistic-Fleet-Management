package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.PackageRequest;
import com.logistics.dto.response.ShipmentResponse.PackageResponse;
import com.logistics.entity.Shipment;
import com.logistics.entity.ShipmentPackage;
import com.logistics.entity.enums.AuditAction;
import com.logistics.entity.enums.PackageStatus;
import com.logistics.entity.enums.ShipmentStatus;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.InvalidStatusTransitionException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.PackageRepository;
import com.logistics.security.SecurityUtils;
import com.logistics.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    private final ShipmentServiceImpl shipmentService;
    private final ShipmentStatusManager statusManager;

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse> list(Long shipmentId) {
        shipmentService.findAllowed(shipmentId);
        return packageRepository.findByShipment_IdOrderByIdAsc(shipmentId).stream().map(ShipmentMapper::toPackage).toList();
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "PACKAGES", entity = "Package")
    public PackageResponse add(Long shipmentId, PackageRequest r) {
        Shipment s = shipmentService.findAllowed(shipmentId);
        checkEditable(s);
        ShipmentPackage p = new ShipmentPackage();
        ShipmentServiceImpl.applyPackage(p, r);
        int next = s.getPackages().stream().map(ShipmentPackage::getPackageCode)
                .mapToInt(c -> Integer.parseInt(c.substring(c.lastIndexOf("-P") + 2))).max().orElse(0) + 1;
        p.setPackageCode(s.getTrackingNumber() + "-P" + next);
        s.addPackage(p);
        ShipmentServiceImpl.recalculate(s);
        packageRepository.save(p);
        return ShipmentMapper.toPackage(p);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "PACKAGES", entity = "Package")
    public PackageResponse update(Long packageId, PackageRequest r) {
        ShipmentPackage p = find(packageId);
        Shipment s = shipmentService.findAllowed(p.getShipment().getId());
        checkEditable(s);
        ShipmentServiceImpl.applyPackage(p, r);
        ShipmentServiceImpl.recalculate(s);
        return ShipmentMapper.toPackage(p);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "PACKAGES", entity = "Package")
    public PackageResponse changeStatus(Long packageId, PackageStatus status, String remarks) {
        if (!SecurityUtils.hasAuthority("WAREHOUSE_OPS")) {
            throw new ForbiddenException("Only warehouse staff can change package status");
        }
        ShipmentPackage p = find(packageId);
        if (p.getCurrentStatus() == PackageStatus.DELIVERED) {
            throw new InvalidStatusTransitionException("Package is already delivered");
        }
        statusManager.changePackageStatus(p, status, p.getShipment().getOriginWarehouse(), remarks);
        return ShipmentMapper.toPackage(p);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, module = "PACKAGES", entity = "Package")
    public void delete(Long packageId) {
        ShipmentPackage p = find(packageId);
        Shipment s = shipmentService.findAllowed(p.getShipment().getId());
        checkEditable(s);
        if (s.getPackages().size() <= 1) {
            throw new BadRequestException("A shipment needs at least one package");
        }
        s.getPackages().remove(p);
        ShipmentServiceImpl.recalculate(s);
    }

    /** Customers may edit only while CREATED; warehouse staff until the shipment leaves. */
    private void checkEditable(Shipment s) {
        boolean warehouse = SecurityUtils.hasAuthority("WAREHOUSE_OPS");
        if (warehouse) {
            if (!ShipmentStatus.WAREHOUSE_QUEUE.contains(s.getCurrentStatus())) {
                throw new InvalidStatusTransitionException("Packages cannot change after dispatch");
            }
            return;
        }
        shipmentService.checkCanEdit(s);
        if (s.getCurrentStatus() != ShipmentStatus.CREATED) {
            throw new InvalidStatusTransitionException("Packages can only be changed before the warehouse receives the shipment");
        }
    }

    private ShipmentPackage find(Long id) {
        return packageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Package", id));
    }
}
