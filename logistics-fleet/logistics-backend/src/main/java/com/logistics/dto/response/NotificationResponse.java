package com.logistics.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String title, String message, String type,
                                   String referenceType, Long referenceId, boolean read,
                                   LocalDateTime readAt, LocalDateTime createdAt) {
}
