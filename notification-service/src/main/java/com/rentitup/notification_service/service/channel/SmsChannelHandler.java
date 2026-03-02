package com.rentitup.notification_service.service.channel;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsChannelHandler implements NotificationChannelHandler {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    // Phone number pattern: starts with + followed by 10-15 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{9,14}$");

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationEntity notification) throws NotificationSendException {
        if (!smsEnabled) {
            log.warn("SMS is disabled. Would have sent to: {}", notification.getRecipient());
            throw new NotificationSendException("SMS channel is currently disabled");
        }

        // TODO: Integrate with SMS provider (Twilio, Africa's Talking, etc.)
        // Example integration:
        //
        // try {
        //     smsClient.send(
        //         notification.getRecipient(),
        //         notification.getBody()
        //     );
        //     log.info("SMS sent to: {}", notification.getRecipient());
        // } catch (Exception e) {
        //     throw new NotificationSendException("SMS send failed", e);
        // }

        log.info("SMS would be sent to: {} with message: {}",
                notification.getRecipient(),
                truncateForLog(notification.getBody()));

        throw new NotificationSendException("SMS provider not yet configured");
    }

    @Override
    public boolean validateRecipient(String recipient) {
        return recipient != null && PHONE_PATTERN.matcher(recipient).matches();
    }

    private String truncateForLog(String message) {
        if (message == null) return "";
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
}
