package com.rentitup.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED
    }

    public enum NotificationPriority {
        LOW, NORMAL, HIGH, URGENT
    }
}
