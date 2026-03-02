package com.rentitup.notification_service.template;

import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel.*;

@Component
@Slf4j
public class TemplateRegistry {

    private final Map<String, TemplateDefinition> templates = new HashMap<>();

    @PostConstruct
    public void init() {
        registerDefaultTemplates();
        log.info("Registered {} notification templates", templates.size());
    }

    private void registerDefaultTemplates() {
        // ==================== Booking Templates ====================

        register(TemplateDefinition.builder()
                .key("booking_confirmed")
                .name("Booking Confirmation")
                .description("Sent when a booking is confirmed")
                .channel(EMAIL)
                .subjectTemplate("Your booking #{{bookingId}} is confirmed!")
                .requiredFields(List.of("userName", "bookingId", "machineName", "startDate", "endDate", "totalAmount"))
                .build());

        register(TemplateDefinition.builder()
                .key("booking_confirmed")
                .name("Booking Confirmation SMS")
                .channel(SMS)
                .subjectTemplate("")
                .bodyTemplate("Hi {{userName}}, your booking #{{bookingId}} for {{machineName}} is confirmed. Pickup: {{startDate}}")
                .requiredFields(List.of("userName", "bookingId", "machineName", "startDate"))
                .build());

        register(TemplateDefinition.builder()
                .key("booking_cancelled")
                .name("Booking Cancellation")
                .channel(EMAIL)
                .subjectTemplate("Booking #{{bookingId}} has been cancelled")
                .requiredFields(List.of("userName", "bookingId", "machineName", "reason"))
                .build());

        register(TemplateDefinition.builder()
                .key("booking_reminder")
                .name("Booking Reminder")
                .channel(EMAIL)
                .subjectTemplate("Reminder: Your booking starts {{startDate}}")
                .requiredFields(List.of("userName", "bookingId", "machineName", "startDate", "pickupLocation"))
                .build());

        register(TemplateDefinition.builder()
                .key("booking_reminder")
                .name("Booking Reminder SMS")
                .channel(SMS)
                .bodyTemplate("Reminder: Your {{machineName}} booking starts {{startDate}}. Pickup at {{pickupLocation}}")
                .requiredFields(List.of("machineName", "startDate", "pickupLocation"))
                .build());

        // ==================== Owner Notification Templates ====================

        register(TemplateDefinition.builder()
                .key("new_booking_request")
                .name("New Booking Request")
                .description("Sent to owner when someone books their machine")
                .channel(EMAIL)
                .subjectTemplate("New booking request for {{machineName}}")
                .requiredFields(List.of("ownerName", "customerName", "machineName", "startDate", "endDate", "totalAmount"))
                .build());

        register(TemplateDefinition.builder()
                .key("new_booking_request")
                .name("New Booking Request SMS")
                .channel(SMS)
                .bodyTemplate("New booking: {{customerName}} booked {{machineName}} from {{startDate}} to {{endDate}}. Amount: {{totalAmount}}")
                .requiredFields(List.of("customerName", "machineName", "startDate", "endDate", "totalAmount"))
                .build());

        // ==================== Maintenance Templates ====================

        register(TemplateDefinition.builder()
                .key("maintenance_reminder")
                .name("Maintenance Reminder")
                .description("Sent to remind owners about upcoming maintenance")
                .channel(EMAIL)
                .subjectTemplate("Maintenance due for {{machineName}}")
                .requiredFields(List.of("ownerName", "machineName", "nextServiceDate", "lastServiceDate"))
                .build());

        register(TemplateDefinition.builder()
                .key("maintenance_reminder")
                .name("Maintenance Reminder SMS")
                .channel(SMS)
                .bodyTemplate("Maintenance due: {{machineName}} needs service by {{nextServiceDate}}")
                .requiredFields(List.of("machineName", "nextServiceDate"))
                .build());

        // ==================== Review Templates ====================

        register(TemplateDefinition.builder()
                .key("review_request")
                .name("Review Request")
                .description("Sent after a completed booking to request a review")
                .channel(EMAIL)
                .subjectTemplate("How was your experience with {{machineName}}?")
                .requiredFields(List.of("userName", "machineName", "bookingId", "reviewLink"))
                .build());

        register(TemplateDefinition.builder()
                .key("new_review_received")
                .name("New Review Received")
                .description("Sent to owner when they receive a review")
                .channel(EMAIL)
                .subjectTemplate("You received a new {{rating}}-star review!")
                .requiredFields(List.of("ownerName", "machineName", "rating", "reviewText", "customerName"))
                .build());

        // ==================== Account Templates ====================

        register(TemplateDefinition.builder()
                .key("welcome")
                .name("Welcome Email")
                .description("Sent when a new user registers")
                .channel(EMAIL)
                .subjectTemplate("Welcome to RentItUp, {{userName}}!")
                .requiredFields(List.of("userName"))
                .build());

        register(TemplateDefinition.builder()
                .key("password_reset")
                .name("Password Reset")
                .channel(EMAIL)
                .subjectTemplate("Reset your RentItUp password")
                .requiredFields(List.of("userName", "resetLink", "expiresIn"))
                .build());

        register(TemplateDefinition.builder()
                .key("kyc_approved")
                .name("KYC Approved")
                .channel(EMAIL)
                .subjectTemplate("Your account has been verified!")
                .requiredFields(List.of("userName"))
                .build());

        register(TemplateDefinition.builder()
                .key("kyc_rejected")
                .name("KYC Rejected")
                .channel(EMAIL)
                .subjectTemplate("Verification update for your account")
                .requiredFields(List.of("userName", "reason"))
                .build());

        // ==================== Payment Templates ====================

        register(TemplateDefinition.builder()
                .key("payment_received")
                .name("Payment Received")
                .channel(EMAIL)
                .subjectTemplate("Payment of {{amount}} received")
                .requiredFields(List.of("userName", "amount", "bookingId", "paymentMethod"))
                .build());

        register(TemplateDefinition.builder()
                .key("payment_failed")
                .name("Payment Failed")
                .channel(EMAIL)
                .subjectTemplate("Payment failed for booking #{{bookingId}}")
                .requiredFields(List.of("userName", "bookingId", "amount", "reason"))
                .build());

        register(TemplateDefinition.builder()
                .key("payout_sent")
                .name("Payout Sent")
                .description("Sent to owner when payout is processed")
                .channel(EMAIL)
                .subjectTemplate("Payout of {{amount}} has been sent")
                .requiredFields(List.of("ownerName", "amount", "accountInfo"))
                .build());
    }

    public void register(TemplateDefinition template) {
        String key = buildKey(template.key(), template.channel());
        templates.put(key, template);
        log.debug("Registered template: {} for channel: {}", template.key(), template.channel());
    }

    public Optional<TemplateDefinition> getTemplate(String templateKey, NotificationChannel channel) {
        return Optional.ofNullable(templates.get(buildKey(templateKey, channel)));
    }

    public List<TemplateDefinition> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    public List<TemplateDefinition> getTemplatesByChannel(NotificationChannel channel) {
        return templates.values().stream()
                .filter(t -> t.channel() == channel)
                .toList();
    }

    private String buildKey(String templateKey, NotificationChannel channel) {
        return templateKey + ":" + channel.name();
    }
}
