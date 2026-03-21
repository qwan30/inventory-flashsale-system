package com.codex.flashsale.alerts;

import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OpsAlertSeverity;
import com.codex.flashsale.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.alerts.delivery.slack.enabled", havingValue = "true")
public class SlackAlertDeliveryPublisher implements AlertDeliveryPublisher {

    private final RestClient restClient;
    private final String webhookUrl;
    private final OpsAlertSeverity minimumSeverity;

    public SlackAlertDeliveryPublisher(ApplicationProperties applicationProperties) {
        ApplicationProperties.Slack slackProperties = applicationProperties.getAlerts().getDelivery().getSlack();
        this.webhookUrl = slackProperties.getWebhookUrl();
        this.minimumSeverity = OpsAlertSeverity.valueOf(slackProperties.getMinimumSeverity().trim().toUpperCase());
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory(slackProperties.getConnectTimeout(), slackProperties.getReadTimeout()))
                .build();
    }

    @Override
    public void publish(OpsAlertResponse alert, AlertDispatchType dispatchType, Instant dispatchedAt) {
        if (!supports(alert.severity())) {
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text",
                            "[%s] %s %s%n%s%ncurrent=%s threshold=%s dispatch=%s observedAt=%s".formatted(
                                    alert.severity(),
                                    alert.code(),
                                    alert.status(),
                                    alert.message(),
                                    alert.currentValue(),
                                    alert.threshold(),
                                    dispatchType,
                                    alert.observedAt()
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Slack alert delivery connection failed: " + exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Slack alert delivery configuration is invalid: " + exception.getMessage(), exception);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Slack alert delivery rejected the request: " + exception.getMessage(), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Slack alert delivery request failed: " + exception.getMessage(), exception);
        }
    }

    private boolean supports(OpsAlertSeverity severity) {
        return severity.ordinal() >= minimumSeverity.ordinal();
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }
}
