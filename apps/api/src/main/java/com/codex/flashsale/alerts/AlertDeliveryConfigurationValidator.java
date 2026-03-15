package com.codex.flashsale.alerts;

import com.codex.flashsale.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.alerts.delivery.enabled", havingValue = "true")
public class AlertDeliveryConfigurationValidator {

    private final ApplicationProperties.AlertDelivery deliveryProperties;

    public AlertDeliveryConfigurationValidator(ApplicationProperties applicationProperties) {
        this.deliveryProperties = applicationProperties.getAlerts().getDelivery();
    }

    @PostConstruct
    void validate() {
        requireNonBlank(deliveryProperties.getWebhookUrl(), "app.alerts.delivery.webhook-url");
        requirePositiveDuration(deliveryProperties.getConnectTimeout(), "app.alerts.delivery.connect-timeout");
        requirePositiveDuration(deliveryProperties.getReadTimeout(), "app.alerts.delivery.read-timeout");
        requirePositiveDuration(deliveryProperties.getReminderInterval(), "app.alerts.delivery.reminder-interval");
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required alert delivery configuration: " + propertyName);
        }
    }

    private void requirePositiveDuration(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("Alert delivery duration must be positive: " + propertyName);
        }
    }
}
