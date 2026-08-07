package com.rentitup.notification_service.service.channel;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;

public interface NotificationChannelHandler {

    NotificationChannel getChannel();

    void send(NotificationEntity notification) throws NotificationSendException;

    boolean validateRecipient(String recipient);

    class NotificationSendException extends Exception {
        public NotificationSendException(String message) {
            super(message);
        }

        public NotificationSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
