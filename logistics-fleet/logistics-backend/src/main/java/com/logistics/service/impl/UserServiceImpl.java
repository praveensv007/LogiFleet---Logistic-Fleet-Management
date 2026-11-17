package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.UserRequests;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.RoleResponse;
import com.logistics.dto.response.UserResponse;
import com.logistics.entity.Customer;
import com.logistics.entity.Permission;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.entity.Warehouse;
import com.logistics.entity.enums.*;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.*;
import com.logistics.security.SecurityUtils;
import com.logistics.service.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, RoleName role, UserStatus status, int page, int size, String sort) {
        Specification<User> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                ps.add(cb.or(cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (role != null) {
                Join<User, Role> r = root.join("roles");
                ps.add(cb.equal(r.get("name"), role.authority()));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(userRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "createdAt")),
                UserMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(find(id));
    }

    @Override
    @Auditable(action = AuditAction.CREATE, module = "USERS", entity = "User")
    public UserResponse create(UserRequests.Create r) {
        if (r.getRole() == RoleName.DRIVER) {
            throw new BadRequestException("role", "Create drivers from Driver management so their license details are captured");
        }
        String email = r.getEmail().trim().toLowerCase();
        checkUnique(r.getUsername(), email, r.getPhone(), null);
        User u = new User();
        u.setUsername(r.getUsername().trim());
        u.setEmail(email);
        u.setPhone(r.getPhone());
        u.setFullName(r.getFullName().trim());
        u.setPasswordHash(passwordEncoder.encode(r.getTemporaryPassword()));
        u.setMustChangePassword(true);
        u.setStatus(r.getStatus() != null ? r.getStatus() : UserStatus.ACTIVE);
        applyRole(u, r.getRole(), r.getWarehouseId());
        userRepository.save(u);
        if (r.getRole() == RoleName.CUSTOMER) {
            createPlaceholderCustomer(u);
        }
        return UserMapper.toResponse(u);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "USERS", entity = "User")
    public UserResponse update(Long id, UserRequests.Update r) {
        User u = find(id);
        String email = r.getEmail().trim().toLowerCase();
        checkUnique(r.getUsername(), email, r.getPhone(), id);
        String oldRole = u.getPrimaryRole();
        if (!r.getRole().name().equals(oldRole) && (RoleName.DRIVER.name().equals(oldRole) || r.getRole() == RoleName.DRIVER
                || RoleName.CUSTOMER.name().equals(oldRole) || r.getRole() == RoleName.CUSTOMER)) {
            throw new BadRequestException("role", "Driver and customer accounts cannot change role");
        }
        if (id.equals(SecurityUtils.currentUserId()) && r.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("status", "You cannot deactivate your own account");
        }
        u.setUsername(r.getUsername().trim());
        u.setEmail(email);
        u.setPhone(r.getPhone());
        u.setFullName(r.getFullName().trim());
        setStatus(u, r.getStatus());
        applyRole(u, r.getRole(), r.getWarehouseId());
        customerRepository.findByUser_Id(id).ifPresent(c -> {
            c.setFullName(u.getFullName());
            c.setEmail(u.getEmail());
            c.setPhone(u.getPhone());
        });
        return UserMapper.toResponse(u);
    }

    @Override
    @Auditable(action = AuditAction.STATUS_CHANGE, module = "USERS", entity = "User")
    public UserResponse changeStatus(Long id, UserStatus status) {
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BadRequestException("status", "You cannot change your own status");
        }
        User u = find(id);
        setStatus(u, status);
        return UserMapper.toResponse(u);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "USERS", entity = "Password")
    public UserResponse resetPassword(Long id) {
        User u = find(id);
        String temp = generateTempPassword();
        u.setPasswordHash(passwordEncoder.encode(temp));
        u.setMustChangePassword(true);
        u.setFailedLoginAttempts(0);
        if (u.getStatus() == UserStatus.LOCKED) {
            u.setStatus(UserStatus.ACTIVE);
            u.setLockedUntil(null);
        }
        UserResponse res = UserMapper.toResponse(u);
        res.setTemporaryPassword(temp);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll(Sort.by("id")).stream().map(UserMapper::toRole).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse.PermissionResponse> permissions() {
        return permissionRepository.findAll(Sort.by("module", "code")).stream().map(UserMapper::toPermission).toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "ROLES", entity = "Role")
    public RoleResponse updateRolePermissions(Long roleId, UserRequests.RolePermissions r) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        List<Permission> perms = permissionRepository.findAllById(r.getPermissionIds());
        if (role.getName().equals(RoleName.ADMIN.authority())
                && perms.stream().noneMatch(p -> p.getCode().equals("ROLE_MANAGE"))) {
            throw new BadRequestException("permissionIds", "Admin must keep ROLE_MANAGE, or nobody could edit roles again");
        }
        role.setPermissions(new HashSet<>(perms));
        if (r.getDescription() != null) {
            role.setDescription(r.getDescription());
        }
        return UserMapper.toRole(role);
    }

    // ---------- helpers ----------

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void applyRole(User u, RoleName roleName, Long warehouseId) {
        Role role = roleRepository.findByName(roleName.authority())
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
        u.setRoles(new HashSet<>(Set.of(role)));
        if (roleName == RoleName.WAREHOUSE_STAFF) {
            if (warehouseId == null) {
                throw new BadRequestException("warehouseId", "Assigned warehouse is required for warehouse staff");
            }
            Warehouse w = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", warehouseId));
            if (w.getStatus() != ActiveStatus.ACTIVE) {
                throw new BadRequestException("warehouseId", "Choose an active warehouse");
            }
            u.setWarehouse(w);
        } else {
            u.setWarehouse(null);
        }
    }

    private void setStatus(User u, UserStatus status) {
        u.setStatus(status);
        if (status == UserStatus.ACTIVE) {
            u.setLockedUntil(null);
            u.setFailedLoginAttempts(0);
        } else if (status == UserStatus.LOCKED) {
            u.setLockedUntil(null); // admin lock: until unlocked by admin
        }
    }

    private void createPlaceholderCustomer(User u) {
        Customer c = new Customer();
        c.setUser(u);
        c.setFullName(u.getFullName());
        c.setEmail(u.getEmail());
        c.setPhone(u.getPhone());
        c.setCustomerType(CustomerType.INDIVIDUAL);
        c.setAddressLine1("To be updated");
        c.setCity("To be updated");
        c.setState("To be updated");
        c.setPincode("000000");
        c.setCountry("India");
        customerRepository.save(c);
        c.setCustomerCode(String.format("CUS%06d", c.getId()));
    }

    private void checkUnique(String username, String email, String phone, Long excludeId) {
        boolean usernameTaken = excludeId == null ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);
        if (usernameTaken) throw new BadRequestException("username", "Username is already taken");
        boolean emailTaken = excludeId == null ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId);
        if (emailTaken) throw new BadRequestException("email", "Email is already registered");
        boolean phoneTaken = excludeId == null ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, excludeId);
        if (phoneTaken) throw new BadRequestException("phone", "Phone is already registered");
    }

    static String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ", lower = "abcdefghjkmnpqrstuvwxyz", digits = "23456789", special = "@#$%&*";
        SecureRandom rnd = new SecureRandom();
        List<Character> chars = new ArrayList<>();
        for (String set : new String[]{upper, lower, digits, special}) {
            chars.add(set.charAt(rnd.nextInt(set.length())));
        }
        String all = upper + lower + digits;
        while (chars.size() < 10) {
            chars.add(all.charAt(rnd.nextInt(all.length())));
        }
        Collections.shuffle(chars, rnd);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }
}
