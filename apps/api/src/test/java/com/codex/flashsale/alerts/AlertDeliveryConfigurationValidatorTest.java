package com.codex.flashsale.alerts;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.config.ApplicationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AlertDeliveryConfigurationValidatorTest {

    @Test
    void shouldRejectMissingWebhookUrlWhenDeliveryIsEnabled() {
        ApplicationProperties properties = createValidProperties();
        properties.getAlerts().getDelivery().setWebhookUrl(null);

        AlertDeliveryConfigurationValidator validator = new AlertDeliveryConfigurationValidator(properties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.alerts.delivery.webhook-url");
    }

    @Test
    void shouldAcceptFullySpecifiedAlertDeliveryConfiguration() {
        ApplicationProperties properties = createValidProperties();

        AlertDeliveryConfigurationValidator validator = new AlertDeliveryConfigurationValidator(properties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private ApplicationProperties createValidProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        ApplicationProperties.AlertDelivery delivery = properties.getAlerts().getDelivery();
        delivery.setEnabled(true);
        delivery.setWebhookUrl("http://localhost:8081/alerts");
        delivery.setConnectTimeout(Duration.ofSeconds(1));
        delivery.setReadTimeout(Duration.ofSeconds(1));
        delivery.setReminderInterval(Duration.ofMinutes(5));
        return properties;
    }
}
