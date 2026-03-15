package com.codex.flashsale.alerts;

import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.alerts.delivery.enabled", havingValue = "true")
public class WebhookAlertDeliveryPublisher implements AlertDeliveryPublisher {

    private static final String SOURCE = "inventory-flashsale-api";

    private final RestClient restClient;
    private final String webhookUrl;

    public WebhookAlertDeliveryPublisher(ApplicationProperties applicationProperties) {
        ApplicationProperties.AlertDelivery deliveryProperties = applicationProperties.getAlerts().getDelivery();
        this.webhookUrl = deliveryProperties.getWebhookUrl();
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory(
                        deliveryProperties.getConnectTimeout(),
                        deliveryProperties.getReadTimeout()
                ))
                .build();
    }

    @Override
    public void publish(OpsAlertResponse alert, AlertDispatchType dispatchType, Instant dispatchedAt) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AlertDeliveryPayload(
                            SOURCE,
                            alert.code(),
                            alert.severity(),
                            alert.status(),
                            alert.message(),
                            alert.currentValue(),
                            alert.threshold(),
                            alert.observedAt(),
                            dispatchType,
                            dispatchedAt
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Alert webhook connection failed: " + exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Alert webhook configuration is invalid: " + exception.getMessage(), exception);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Alert webhook rejected the request: " + exception.getMessage(), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Alert webhook request failed: " + exception.getMessage(), exception);
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }

}
