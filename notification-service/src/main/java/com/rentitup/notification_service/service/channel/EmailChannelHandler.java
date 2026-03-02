package com.rentitup.notification_service.service.channel;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailChannelHandler implements NotificationChannelHandler {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@rentitup.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:RentItUp}")
    private String fromName;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationEntity notification) throws NotificationSendException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true); // true = HTML

            // Set priority header
            if (notification.getPriority() == NotificationEntity.NotificationPriority.URGENT ||
                notification.getPriority() == NotificationEntity.NotificationPriority.HIGH) {
                message.setHeader("X-Priority", "1");
                message.setHeader("Importance", "high");
            }

            mailSender.send(message);
            log.info("Email sent successfully to: {}", notification.getRecipient());

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", notification.getRecipient(), e);
            throw new NotificationSendException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", notification.getRecipient(), e);
            throw new NotificationSendException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateRecipient(String recipient) {
        return recipient != null && EMAIL_PATTERN.matcher(recipient).matches();
    }
}
