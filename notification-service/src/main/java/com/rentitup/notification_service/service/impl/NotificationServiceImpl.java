package com.rentitup.notification_service.service.impl;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationPriority;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationStatus;
import com.rentitup.notification_service.repository.NotificationRepository;
import com.rentitup.notification_service.service.NotificationService;
import com.rentitup.notification_service.service.channel.NotificationChannelHandler;
import com.rentitup.notification_service.service.channel.NotificationChannelHandler.NotificationSendException;
import com.rentitup.notification_service.template.NotificationTemplateService;
import com.rentitup.notification_service.template.NotificationTemplateService.RenderedTemplate;
import com.rentitup.common.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final Map<NotificationChannel, NotificationChannelHandler> channelHandlers;

    private static final int MAX_RETRY_COUNT = 3;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationTemplateService templateService,
            List<NotificationChannelHandler> handlers
    ) {
        this.notificationRepository = notificationRepository;
        this.templateService = templateService;

        // Build a map of channel handlers
        this.channelHandlers = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelHandler handler : handlers) {
            channelHandlers.put(handler.getChannel(), handler);
        }
        log.info("Initialized notification service with {} channel handlers", channelHandlers.size());
    }

    @Override
    @Transactional
    public NotificationEntity send(
            String recipient,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            UUID userId,
            NotificationPriority priority
    ) {
        log.info("Sending {} notification to {} using template: {}", channel, recipient, templateKey);

        // Validate channel handler exists
        NotificationChannelHandler handler = getHandler(channel);

        // Validate recipient format
        if (!handler.validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid recipient format for channel " + channel + ": " + recipient);
        }

        // Render template
        RenderedTemplate rendered = templateService.render(templateKey, channel, data);

        // Create notification entity
        NotificationEntity notification = NotificationEntity.builder()
                .userId(userId)
                .recipient(recipient)
                .channel(channel)
                .templateKey(templateKey)
                .data(data != null ? new HashMap<>(data) : new HashMap<>())
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .subject(rendered.subject())
                .body(rendered.body())
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // Send asynchronously
        sendAsync(notification, handler);

        return notification;
    }

    @Async
    protected void sendAsync(NotificationEntity notification, NotificationChannelHandler handler) {
        try {
            handler.send(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification {} sent successfully", notification.getId());
        } catch (NotificationSendException e) {
            log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
        }
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public List<NotificationEntity> sendBulk(List<NotificationRequest> requests) {
        log.info("Processing bulk notification request with {} items", requests.size());

        List<NotificationEntity> results = new ArrayList<>();
        for (NotificationRequest request : requests) {
            try {
                NotificationEntity notification = send(
                        request.recipient(),
                        request.channel(),
                        request.templateKey(),
                        request.data(),
                        request.userId(),
                        request.priority()
                );
                results.add(notification);
            } catch (Exception e) {
                log.error("Failed to send notification to {}: {}", request.recipient(), e.getMessage());
                // Create a failed notification record
                NotificationEntity failed = NotificationEntity.builder()
                        .recipient(request.recipient())
                        .channel(request.channel())
                        .templateKey(request.templateKey())
                        .status(NotificationStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                results.add(notificationRepository.save(failed));
            }
        }

        return results;
    }

    @Override
    @Transactional
    public String broadcast(
            List<String> recipients,
            NotificationChannel channel,
            String templateKey,
            Map<String, String> data,
            NotificationPriority priority
    ) {
        String batchId = UUID.randomUUID().toString();
        log.info("Broadcasting notification to {} recipients, batchId: {}", recipients.size(), batchId);

        // Queue all notifications for async sending
        for (String recipient : recipients) {
            try {
                send(recipient, channel, templateKey, data, null, priority);
            } catch (Exception e) {
                log.error("Failed to queue notification for {}: {}", recipient, e.getMessage());
            }
        }

        return batchId;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationEntity getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationEntity> listNotifications(
            UUID userId,
            NotificationChannel channel,
            NotificationStatus status,
            Pageable pageable
    ) {
        return notificationRepository.findWithFilters(userId, channel, status, pageable);
    }

    @Override
    @Transactional
    public NotificationEntity retry(UUID notificationId) {
        NotificationEntity notification = getNotification(notificationId);

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed notifications");
        }

        if (notification.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new IllegalStateException("Maximum retry count exceeded");
        }

        NotificationChannelHandler handler = getHandler(notification.getChannel());

        notification.setStatus(NotificationStatus.PENDING);
        notification.setErrorMessage(null);
        notificationRepository.save(notification);

        sendAsync(notification, handler);

        return notification;
    }

    private NotificationChannelHandler getHandler(NotificationChannel channel) {
        NotificationChannelHandler handler = channelHandlers.get(channel);
        if (handler == null) {
            throw new UnsupportedOperationException("No handler configured for channel: " + channel);
        }
        return handler;
    }
}
