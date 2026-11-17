package com.logistics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dto.response.AuditLogResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.AuditLog;
import com.logistics.entity.User;
import com.logistics.entity.enums.AuditAction;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.AuditLogRepository;
import com.logistics.repository.UserRepository;
import com.logistics.security.SecurityUtils;
import com.logistics.security.UserPrincipal;
import com.logistics.service.AuditService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void log(AuditAction action, String module, String entityName, Long entityId, Object oldValue, Object newValue) {
        AuditLog entry = new AuditLog();
        UserPrincipal p = SecurityUtils.currentPrincipal().orElse(null);
        if (p != null) {
            entry.setUser(userRepository.getReferenceById(p.getId()));
            entry.setUsername(p.getUsername());
            entry.setRole(p.getRole());
        } else {
            entry.setUsername("system");
        }
        fill(entry, action, module, entityName, entityId, oldValue, newValue);
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional
    public void logFor(User user, AuditAction action, String module) {
        AuditLog entry = new AuditLog();
        entry.setUser(userRepository.getReferenceById(user.getId()));
        entry.setUsername(user.getUsername());
        entry.setRole(user.getPrimaryRole());
        fill(entry, action, module, "User", user.getId(), null, null);
        auditLogRepository.save(entry);
    }

    private void fill(AuditLog entry, AuditAction action, String module, String entityName, Long entityId,
                      Object oldValue, Object newValue) {
        entry.setAction(action);
        entry.setModule(module);
        entry.setEntityName(entityName);
        entry.setEntityId(entityId);
        entry.setOldValue(toJson(oldValue));
        entry.setNewValue(toJson(newValue));
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            entry.setIpAddress(forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr());
            String agent = req.getHeader("User-Agent");
            entry.setUserAgent(agent != null && agent.length() > 255 ? agent.substring(0, 255) : agent);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.debug("Could not serialise audit value: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Long userId, String username, String module, AuditAction action,
                                                 LocalDate from, LocalDate to, int page, int size) {
        Specification<AuditLog> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (userId != null) ps.add(cb.equal(root.get("user").get("id"), userId));
            if (username != null && !username.isBlank())
                ps.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            if (module != null && !module.isBlank()) ps.add(cb.equal(root.get("module"), module));
            if (action != null) ps.add(cb.equal(root.get("action"), action));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) ps.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(auditLogRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), UserMapper::toAudit);
    }
}
