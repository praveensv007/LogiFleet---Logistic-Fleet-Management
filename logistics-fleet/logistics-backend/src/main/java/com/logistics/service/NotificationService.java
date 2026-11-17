package com.logistics.service;

import com.logistics.dto.response.NotificationResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.User;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.entity.enums.RoleName;

public interface NotificationService {

    /** Saves a notifications row and pushes it on /user/queue/notifications. */
    void notifyUser(User user, String title, String message, NotificationType type, ReferenceType refType, Long refId);

    void notifyRole(RoleName role, String title, String message, NotificationType type, ReferenceType refType, Long refId);

    /** Staff assigned to the warehouse plus its manager. */
    void notifyWarehouse(Long warehouseId, String title, String message, NotificationType type, ReferenceType refType, Long refId);

    PageResponse<NotificationResponse> myNotifications(boolean unreadOnly, int page, int size);

    long unreadCount();

    NotificationResponse markRead(Long id);

    int markAllRead();
}
