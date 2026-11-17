package com.logistics.service;

import com.logistics.dto.response.AuditLogResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.User;
import com.logistics.entity.enums.AuditAction;

import java.time.LocalDate;

public interface AuditService {

    /** Uses the logged-in user from the security context. Values are serialised to JSON. */
    void log(AuditAction action, String module, String entityName, Long entityId, Object oldValue, Object newValue);

    /** For LOGIN / LOGOUT where the security context is not set yet. */
    void logFor(User user, AuditAction action, String module);

    PageResponse<AuditLogResponse> search(Long userId, String username, String module, AuditAction action,
                                          LocalDate from, LocalDate to, int page, int size);
}
