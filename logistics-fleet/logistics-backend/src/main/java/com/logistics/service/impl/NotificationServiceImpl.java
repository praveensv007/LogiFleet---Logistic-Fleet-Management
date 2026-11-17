package com.logistics.service.impl;

import com.logistics.dto.response.NotificationResponse;
import com.logistics.dto.response.PageResponse;
import com.logistics.entity.Notification;
import com.logistics.entity.User;
import com.logistics.entity.Warehouse;
import com.logistics.entity.enums.NotificationType;
import com.logistics.entity.enums.ReferenceType;
import com.logistics.entity.enums.RoleName;
import com.logistics.entity.enums.UserStatus;
import com.logistics.exception.ForbiddenException;
import com.logistics.exception.ResourceNotFoundException;
import com.logistics.mapper.UserMapper;
import com.logistics.repository.NotificationRepository;
import com.logistics.repository.UserRepository;
import com.logistics.repository.WarehouseRepository;
import com.logistics.security.SecurityUtils;
import com.logistics.service.NotificationService;
import com.logistics.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final WebSocketPublisher publisher;

    @Override
    public void notifyUser(User user, String title, String message, NotificationType type,
                           ReferenceType refType, Long refId) {
        if (user == null || user.getStatus() == UserStatus.INACTIVE) {
            return;
        }
        Notification n = new Notification();
        n.setRecipientUser(user);
        n.setTitle(truncate(title, 150));
        n.setMessage(truncate(message, 500));
        n.setType(type);
        n.setReferenceType(refType);
        n.setReferenceId(refId);
        notificationRepository.save(n);
        publisher.userNotification(user.getUsername(), UserMapper.toNotification(n));
    }

    @Override
    public void notifyRole(RoleName role, String title, String message, NotificationType type,
                           ReferenceType refType, Long refId) {
        userRepository.findByRoles_Name(role.authority())
                .forEach(u -> notifyUser(u, title, message, type, refType, refId));
    }

    @Override
    public void notifyWarehouse(Long warehouseId, String title, String message, NotificationType type,
                                ReferenceType refType, Long refId) {
        if (warehouseId == null) {
            return;
        }
        Map<Long, User> recipients = new LinkedHashMap<>();
        userRepository.findByWarehouse_Id(warehouseId).forEach(u -> recipients.put(u.getId(), u));
        Warehouse w = warehouseRepository.findById(warehouseId).orElse(null);
        if (w != null && w.getManager() != null) {
            recipients.put(w.getManager().getId(), w.getManager());
        }
        recipients.values().forEach(u -> notifyUser(u, title, message, type, refType, refId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> myNotifications(boolean unreadOnly, int page, int size) {
        Long userId = SecurityUtils.current().getId();
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(unreadOnly
                ? notificationRepository.findByRecipientUser_IdAndReadFalse(userId, pr)
                : notificationRepository.findByRecipientUser_Id(userId, pr), UserMapper::toNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientUser_IdAndReadFalse(SecurityUtils.current().getId());
    }

    @Override
    public NotificationResponse markRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!n.getRecipientUser().getId().equals(SecurityUtils.current().getId())) {
            throw new ForbiddenException("This notification belongs to another user");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        }
        return UserMapper.toNotification(n);
    }

    @Override
    public int markAllRead() {
        return notificationRepository.markAllRead(SecurityUtils.current().getId(), LocalDateTime.now());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
