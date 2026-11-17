package com.logistics.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, LocalDateTime time, Long userId, String username, String role,
                               String action, String module, String entityName, Long entityId,
                               String oldValue, String newValue, String ipAddress, String userAgent) {
}
