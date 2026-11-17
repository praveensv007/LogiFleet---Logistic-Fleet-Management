package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.DriverRequest;
import com.logistics.dto.response.DriverResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.Driver;
import com.logistics.entity.User;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.FleetMapper;
import com.logistics.mapper.MasterDataMapper;
import com.logistics.repository.DriverRepository;
import com.logistics.repository.FleetAssignmentRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.DriverService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FleetAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> list(String search, DriverAvailability availability, int page, int size, String sort) {
        Specification<Driver> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                ps.add(cb.or(cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("driverCode")), like),
                        cb.like(cb.lower(root.get("licenseNumber")), like),
                        cb.like(root.get("phone"), like)));
            }
            if (availability != null) ps.add(cb.equal(root.get("availabilityStatus"), availability));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(driverRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "createdAt")),
                MasterDataMapper::toDriver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> available() {
        return driverRepository.findByAvailabilityStatusAndLicenseExpiryDateAfterOrderByFullNameAsc(
                        DriverAvailability.AVAILABLE, LocalDate.now()).stream()
                .filter(d -> d.getUser() == null || d.getUser().getStatus() == UserStatus.ACTIVE)
                .map(MasterDataMapper::toDriver).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse get(Long id) {
        Driver d = find(id);
        DriverResponse res = MasterDataMapper.toDriver(d);
        res.setRecentTrips(assignmentRepository.findByDriver_IdOrderByAssignedAtDesc(id, PageRequest.of(0, 10))
                .stream().map(FleetMapper::toAssignment).toList());
        return res;
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "DRIVERS", entity = "Driver")
    public DriverResponse create(DriverRequest r) {
        if (r.getUsername() == null || r.getUsername().isBlank()) {
            throw new BadRequestException("username", "Username is required");
        }
        if (r.getTemporaryPassword() == null || r.getTemporaryPassword().isBlank()) {
            throw new BadRequestException("temporaryPassword", "Temporary password is required");
        }
        if (r.getAvailabilityStatus() == DriverAvailability.ON_TRIP) {
            throw new BadRequestException("availabilityStatus", "A new driver cannot start ON_TRIP");
        }
        String email = r.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(r.getUsername().trim()))
            throw new BadRequestException("username", "Username is already taken");
        if (userRepository.existsByEmail(email))
            throw new BadRequestException("email", "Email is already registered");
        if (userRepository.existsByPhone(r.getPhone()) || driverRepository.existsByPhone(r.getPhone()))
            throw new BadRequestException("phone", "Phone is already registered");
        if (driverRepository.existsByLicenseNumber(r.getLicenseNumber().trim().toUpperCase()))
            throw new BadRequestException("licenseNumber", "License number is already registered");
        validate(r);

        User u = new User();
        u.setUsername(r.getUsername().trim());
        u.setEmail(email);
        u.setPhone(r.getPhone());
        u.setFullName(r.getFullName().trim());
        u.setPasswordHash(passwordEncoder.encode(r.getTemporaryPassword()));
        u.setMustChangePassword(true);
        u.setStatus(UserStatus.ACTIVE);
        u.setRoles(new HashSet<>(Set.of(roleRepository.findByName(RoleName.DRIVER.authority())
                .orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.DRIVER)))));
        userRepository.save(u);

        Driver d = new Driver();
        d.setUser(u);
        apply(d, r);
        driverRepository.save(d);
        d.setDriverCode(String.format("DRV-%04d", d.getId()));
        return MasterDataMapper.toDriver(d);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "DRIVERS", entity = "Driver")
    public DriverResponse update(Long id, DriverRequest r) {
        Driver d = find(id);
        String email = r.getEmail().trim().toLowerCase();
        Long userId = d.getUser() != null ? d.getUser().getId() : -1L;
        if (userRepository.existsByEmailAndIdNot(email, userId))
            throw new BadRequestException("email", "Email is already registered");
        if (driverRepository.existsByPhoneAndIdNot(r.getPhone(), id) || userRepository.existsByPhoneAndIdNot(r.getPhone(), userId))
            throw new BadRequestException("phone", "Phone is already registered");
        if (driverRepository.existsByLicenseNumberAndIdNot(r.getLicenseNumber().trim().toUpperCase(), id))
            throw new BadRequestException("licenseNumber", "License number is already registered");
        if (r.getAvailabilityStatus() != d.getAvailabilityStatus()) {
            checkAvailabilityChange(d, r.getAvailabilityStatus());
        }
        validate(r);
        apply(d, r);
        if (d.getUser() != null) {
            d.getUser().setEmail(email);
            d.getUser().setPhone(r.getPhone());
            d.getUser().setFullName(d.getFullName());
            d.getUser().setStatus(r.getAvailabilityStatus() == DriverAvailability.INACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        }
        return MasterDataMapper.toDriver(d);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "DRIVERS", entity = "Driver")
    public DriverResponse changeAvailability(Long id, DriverAvailability availability) {
        Driver d = find(id);
        checkAvailabilityChange(d, availability);
        d.setAvailabilityStatus(availability);
        if (d.getUser() != null) {
            d.getUser().setStatus(availability == DriverAvailability.INACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        }
        return MasterDataMapper.toDriver(d);
    }

    private void checkAvailabilityChange(Driver d, DriverAvailability to) {
        if (to == DriverAvailability.ON_TRIP) {
            throw new BadRequestException("availabilityStatus", "ON_TRIP is set automatically by an assignment");
        }
        boolean activeTrip = !assignmentRepository.findByDriver_IdAndStatusInOrderByAssignedAtDesc(d.getId(),
                EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS)).isEmpty();
        if (activeTrip) {
            throw new BadRequestException("availabilityStatus", "Driver has an active trip; finish or cancel it first");
        }
    }

    private void validate(DriverRequest r) {
        int age = Period.between(r.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 18 || age > 65) {
            throw new BadRequestException("dateOfBirth", "Driver age must be between 18 and 65");
        }
        if (!r.getLicenseExpiryDate().isAfter(r.getLicenseIssueDate())) {
            throw new BadRequestException("licenseExpiryDate", "Expiry must be after the issue date");
        }
    }

    private void apply(Driver d, DriverRequest r) {
        d.setFullName(r.getFullName().trim());
        d.setDateOfBirth(r.getDateOfBirth());
        d.setGender(r.getGender());
        d.setPhone(r.getPhone());
        d.setAddress(r.getAddress());
        d.setCity(r.getCity());
        d.setState(r.getState());
        d.setPincode(r.getPincode());
        d.setLicenseNumber(r.getLicenseNumber().trim().toUpperCase());
        d.setLicenseType(r.getLicenseType());
        d.setLicenseIssueDate(r.getLicenseIssueDate());
        d.setLicenseExpiryDate(r.getLicenseExpiryDate());
        d.setLicenseDocUrl(r.getLicenseDocUrl());
        d.setIdProofType(r.getIdProofType());
        d.setIdProofNumber(r.getIdProofNumber());
        d.setExperienceYears(r.getExperienceYears());
        d.setJoiningDate(r.getJoiningDate());
        d.setEmergencyContactName(r.getEmergencyContactName());
        d.setEmergencyContactPhone(r.getEmergencyContactPhone());
        d.setPhotoUrl(AuthServiceImpl.blankToNull(r.getPhotoUrl()));
        d.setAvailabilityStatus(r.getAvailabilityStatus());
    }

    private Driver find(Long id) {
        return driverRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver", id));
    }
}
