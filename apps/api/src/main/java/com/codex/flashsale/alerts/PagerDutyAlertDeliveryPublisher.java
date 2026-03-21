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
@ConditionalOnProperty(name = "app.alerts.delivery.pager-duty.enabled", havingValue = "true")
public class PagerDutyAlertDeliveryPublisher implements AlertDeliveryPublisher {

    private final RestClient restClient;
    private final String eventsUrl;
    private final String routingKey;
    private final OpsAlertSeverity minimumSeverity;

    public PagerDutyAlertDeliveryPublisher(ApplicationProperties applicationProperties) {
        ApplicationProperties.PagerDuty pagerDutyProperties = applicationProperties.getAlerts().getDelivery().getPagerDuty();
        this.eventsUrl = pagerDutyProperties.getEventsUrl();
        this.routingKey = pagerDutyProperties.getRoutingKey();
        this.minimumSeverity = OpsAlertSeverity.valueOf(pagerDutyProperties.getMinimumSeverity().trim().toUpperCase());
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory(pagerDutyProperties.getConnectTimeout(), pagerDutyProperties.getReadTimeout()))
                .build();
    }

    @Override
    public void publish(OpsAlertResponse alert, AlertDispatchType dispatchType, Instant dispatchedAt) {
        if (!supports(alert.severity())) {
            return;
        }
        String observedAt = alert.observedAt() == null ? "" : alert.observedAt().toString();
        try {
            restClient.post()
                    .uri(eventsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "routing_key", routingKey,
                            "event_action", "trigger",
                            "payload", Map.of(
                                    "summary", alert.code() + ": " + alert.message(),
                                    "severity", alert.severity().name().toLowerCase(),
                                    "source", "inventory-flashsale-api",
                                    "timestamp", dispatchedAt.toString(),
                                    "custom_details", Map.of(
                                            "status", alert.status().name(),
                                            "dispatchType", dispatchType.name(),
                                            "currentValue", alert.currentValue(),
                                            "threshold", alert.threshold(),
                                            "observedAt", observedAt
                                    )
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("PagerDuty alert delivery connection failed: " + exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PagerDuty alert delivery configuration is invalid: " + exception.getMessage(), exception);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("PagerDuty alert delivery rejected the request: " + exception.getMessage(), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("PagerDuty alert delivery request failed: " + exception.getMessage(), exception);
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
