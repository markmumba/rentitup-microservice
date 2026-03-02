package com.rentitup.notification_service.service.channel;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;

public interface NotificationChannelHandler {

    /**
     * Returns the channel this handler supports
     */
    NotificationChannel getChannel();

    /**
     * Sends the notification through this channel
     *
     * @param notification the notification to send (with rendered subject and body)
     * @throws NotificationSendException if sending fails
     */
    void send(NotificationEntity notification) throws NotificationSendException;

    /**
     * Validates the recipient format for this channel
     *
     * @param recipient the recipient address (email, phone, etc.)
     * @return true if valid
     */
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
