package com.rentitup.notification_service.service;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationPriority;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    NotificationEntity send(
            String recipient,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            UUID userId,
            NotificationPriority priority
    );

    List<NotificationEntity> sendBulk(List<NotificationRequest> requests);

    String broadcast(
            List<String> recipients,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            NotificationPriority priority
    );

    NotificationEntity getNotification(UUID notificationId);

    Page<NotificationEntity> listNotifications(
            UUID userId,
            NotificationChannel channel,
            NotificationStatus status,
            Pageable pageable
    );

    NotificationEntity retry(UUID notificationId);

    NotificationEntity markRead(UUID notificationId, UUID userId);

    int markAllRead(UUID userId);

    record NotificationRequest(
            String recipient,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            UUID userId,
            NotificationPriority priority
    ) {}
}
