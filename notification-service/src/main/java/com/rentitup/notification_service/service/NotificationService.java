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

    /**
     * Sends a notification using a template
     */
    NotificationEntity send(
            String recipient,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            UUID userId,
            NotificationPriority priority
    );

    /**
     * Sends multiple notifications
     */
    List<NotificationEntity> sendBulk(List<NotificationRequest> requests);

    /**
     * Broadcasts the same notification to multiple recipients
     */
    String broadcast(
            List<String> recipients,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            NotificationPriority priority
    );

    /**
     * Gets a notification by ID
     */
    NotificationEntity getNotification(UUID notificationId);

    /**
     * Lists notifications with filters
     */
    Page<NotificationEntity> listNotifications(
            UUID userId,
            NotificationChannel channel,
            NotificationStatus status,
            Pageable pageable
    );

    /**
     * Retries a failed notification
     */
    NotificationEntity retry(UUID notificationId);

    /**
     * Request DTO for bulk operations
     */
    record NotificationRequest(
            String recipient,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            UUID userId,
            NotificationPriority priority
    ) {}
}
