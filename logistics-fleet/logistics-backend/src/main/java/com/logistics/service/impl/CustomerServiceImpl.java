package com.logistics.service.impl;

import com.logistics.audit.Auditable;
import com.logistics.dto.request.CustomerUpdateRequest;
import com.logistics.dto.response.CustomerResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.dto.response.ShipmentResponse;
import com.logistics.entity.Customer;
import com.logistics.entity.User;
import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.AuditAction;
import com.logistics.entity.enums.CustomerType;
import com.logistics.entity.enums.UserStatus;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.MasterDataMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.CustomerRepository;
import com.logistics.repository.ShipmentRepository;
import com.logistics.repository.UserRepository;
import com.logistics.security.SecurityUtils;
import com.logistics.security.UserPrincipal;
import com.logistics.service.CustomerService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String search, CustomerType type, ActiveStatus status,
                                               int page, int size, String sort) {
        Specification<Customer> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = PagingUtils.like(search);
                ps.add(cb.or(cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("customerCode")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phone"), like),
                        cb.like(cb.lower(root.get("city")), like)));
            }
            if (type != null) ps.add(cb.equal(root.get("customerType"), type));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(customerRepository.findAll(spec, PagingUtils.pageable(page, size, sort, "createdAt")),
                c -> MasterDataMapper.toCustomer(c, shipmentRepository.countByCustomer_Id(c.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        Customer c = findAllowed(id);
        return MasterDataMapper.toCustomer(c, shipmentRepository.countByCustomer_Id(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse me() {
        Customer c = customerRepository.findByUser_Id(SecurityUtils.current().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No customer profile for this account"));
        return MasterDataMapper.toCustomer(c, shipmentRepository.countByCustomer_Id(c.getId()));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, module = "CUSTOMERS", entity = "Customer")
    public CustomerResponse update(Long id, CustomerUpdateRequest r) {
        Customer c = findAllowed(id);
        boolean staff = SecurityUtils.hasAuthority("CUSTOMER_EDIT");
        if (!staff && !isOwner(c)) {
            throw new ForbiddenException("You can only edit your own profile");
        }
        if (r.getCustomerType() == CustomerType.BUSINESS && (r.getCompanyName() == null || r.getCompanyName().isBlank())) {
            throw new BadRequestException("companyName", "Company name is required for business customers");
        }
        String email = r.getEmail().trim().toLowerCase();
        User u = c.getUser();
        if (u != null) {
            if (userRepository.existsByEmailAndIdNot(email, u.getId())) {
                throw new BadRequestException("email", "Email is already registered");
            }
            if (userRepository.existsByPhoneAndIdNot(r.getPhone(), u.getId())) {
                throw new BadRequestException("phone", "Phone is already registered");
            }
            u.setEmail(email);
            u.setPhone(r.getPhone());
            u.setFullName(r.getFullName().trim());
        }
        c.setFullName(r.getFullName().trim());
        c.setEmail(email);
        c.setPhone(r.getPhone());
        c.setAlternatePhone(AuthServiceImpl.blankToNull(r.getAlternatePhone()));
        c.setCustomerType(r.getCustomerType());
        c.setCompanyName(AuthServiceImpl.blankToNull(r.getCompanyName()));
        c.setGstNumber(AuthServiceImpl.blankToNull(r.getGstNumber()));
        c.setAddressLine1(r.getAddressLine1());
        c.setAddressLine2(AuthServiceImpl.blankToNull(r.getAddressLine2()));
        c.setCity(r.getCity());
        c.setState(r.getState());
        c.setPincode(r.getPincode());
        c.setCountry(r.getCountry());
        if (staff && r.getStatus() != null) {
            c.setStatus(r.getStatus());
            if (u != null) {
                u.setStatus(r.getStatus() == ActiveStatus.ACTIVE ? UserStatus.ACTIVE : UserStatus.INACTIVE);
            }
        }
        return MasterDataMapper.toCustomer(c, shipmentRepository.countByCustomer_Id(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> shipments(Long customerId) {
        findAllowed(customerId);
        return shipmentRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId).stream()
                .map(s -> ShipmentMapper.toResponse(s, false, null)).toList();
    }

    private Customer findAllowed(Long id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (!SecurityUtils.hasAuthority("CUSTOMER_VIEW") && !isOwner(c)) {
            throw new ForbiddenException("You can only view your own profile");
        }
        return c;
    }

    private boolean isOwner(Customer c) {
        UserPrincipal p = SecurityUtils.current();
        return c.getUser() != null && c.getUser().getId().equals(p.getId());
    }
}
