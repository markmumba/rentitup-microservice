package com.rentitup.notification_service.template;

import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final TemplateEngine templateEngine;
    private final TemplateRegistry templateRegistry;

    public record RenderedTemplate(String subject, String body) {}

    public RenderedTemplate render(String templateKey, NotificationChannel channel, Map<String, String> data) {
        TemplateDefinition template = templateRegistry.getTemplate(templateKey, channel)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template not found: " + templateKey + " for channel: " + channel));

        validateRequiredFields(template, data);

        Context context = new Context();
        data.forEach(context::setVariable);

        context.setVariable("appName", "RentItUp");
        context.setVariable("supportEmail", "support@rentitup.com");

        String renderedSubject = renderString(template.getSubjectTemplate(), context);
        String renderedBody;

        if (channel == NotificationChannel.EMAIL) {
            renderedBody = templateEngine.process("email/" + templateKey, context);
        } else {
            renderedBody = renderString(template.getBodyTemplate(), context);
        }

        return new RenderedTemplate(renderedSubject, renderedBody);
    }

    public Optional<TemplateDefinition> getTemplate(String templateKey, NotificationChannel channel) {
        return templateRegistry.getTemplate(templateKey, channel);
    }


    public List<TemplateDefinition> listTemplates(NotificationChannel channel) {
        if (channel == null) {
            return templateRegistry.getAllTemplates();
        }
        return templateRegistry.getTemplatesByChannel(channel);
    }

    private void validateRequiredFields(TemplateDefinition template, Map<String, String> data) {
        List<String> missingFields = template.getRequiredFields().stream()
                .filter(field -> !data.containsKey(field) || data.get(field) == null)
                .toList();

        if (!missingFields.isEmpty()) {
            throw new TemplateMissingFieldsException(
                    "Missing required fields for template " + template.getKey() + ": " + missingFields);
        }
    }

    private String renderString(String template, Context context) {
        if (template == null) return "";

        String result = template;
        for (String varName : context.getVariableNames()) {
            Object value = context.getVariable(varName);
            if (value != null) {
                result = result.replace("{{" + varName + "}}", value.toString());
                result = result.replace("${" + varName + "}", value.toString());
            }
        }
        return result;
    }

    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    public static class TemplateMissingFieldsException extends RuntimeException {
        public TemplateMissingFieldsException(String message) {
            super(message);
        }
    }
}
