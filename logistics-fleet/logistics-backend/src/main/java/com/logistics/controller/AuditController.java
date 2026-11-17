package com.logistics.controller;

import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.AuditLogResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.enums.AuditAction;
import com.logistics.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(auditService.search(userId, user, module, action, from, to, page, Math.min(size, 100)));
    }
}
