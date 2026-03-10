package com.rentitup.notification_service.template;

import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;

import java.util.List;

public record TemplateDefinition(
        String key,
        String name,
        String description,
        NotificationChannel channel,
        String subjectTemplate,
        String bodyTemplate,
        List<String> requiredFields,
        boolean active
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String key;
        private String name;
        private String description;
        private NotificationChannel channel;
        private String subjectTemplate;
        private String bodyTemplate;
        private List<String> requiredFields = List.of();
        private boolean active = true;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder subjectTemplate(String subjectTemplate) {
            this.subjectTemplate = subjectTemplate;
            return this;
        }

        public Builder bodyTemplate(String bodyTemplate) {
            this.bodyTemplate = bodyTemplate;
            return this;
        }

        public Builder requiredFields(List<String> requiredFields) {
            this.requiredFields = requiredFields;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public TemplateDefinition build() {
            return new TemplateDefinition(key, name, description, channel, subjectTemplate, bodyTemplate, requiredFields, active);
        }
    }
}
