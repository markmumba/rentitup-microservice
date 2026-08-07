package com.rentitup.notification_service.template;

import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TemplateDefinition {
    String key;
    String name;
    String description;
    NotificationChannel channel;
    String subjectTemplate;
    String bodyTemplate;

    @Builder.Default
    List<String> requiredFields = List.of();

    @Builder.Default
    boolean active = true;
}
