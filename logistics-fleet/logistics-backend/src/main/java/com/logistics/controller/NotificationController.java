package com.logistics.controller;

import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.NotificationResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(notificationService.myNotifications(unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> read(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> readAll() {
        return ApiResponse.ok("All marked as read", Map.of("updated", notificationService.markAllRead()));
    }
}
