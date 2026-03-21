package com.codex.flashsale.connector.tiktok;

import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "real")
public class TikTokRestChannelClient implements TikTokChannelClient {

    private static final String INVENTORY_PATH_TEMPLATE = "/open_api/v1/inventory/sku/{sku}";

    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;
    private final RestClient restClient;
    private final ApplicationProperties.TikTok properties;

    public TikTokRestChannelClient(
            ObjectMapper objectMapper,
            TimeProvider timeProvider,
            ApplicationProperties applicationProperties
    ) {
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
        this.properties = applicationProperties.getChannel().getTikTok();
        validateConfiguration();
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(createRequestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
    }

    @Override
    public Optional<TikTokListingView> findListingBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        JsonNode response = execute(HttpMethod.GET, INVENTORY_PATH_TEMPLATE.replace("{sku}", sku), null);
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return Optional.empty();
        }
        return Optional.of(new TikTokListingView(
                new TikTokListingReference(
                        sku,
                        requiredText(data, "listing_id", "Missing TikTok listing id"),
                        requiredText(data, "warehouse_id", "Missing TikTok warehouse id")
                ),
                new TikTokRemoteStockView(
                        sku,
                        requiredInt(data, "available_qty", "Missing TikTok available_qty"),
                        requiredInt(data, "reserved_qty", "Missing TikTok reserved_qty")
                )
        ));
    }

    @Override
    public TikTokRemoteStockView updateAvailableStock(TikTokListingReference reference, int availableQty) {
        JsonNode response = execute(
                HttpMethod.POST,
                INVENTORY_PATH_TEMPLATE.replace("{sku}", reference.sku()),
                Map.of(
                        "listing_id", reference.listingId(),
                        "warehouse_id", reference.warehouseId(),
                        "available_qty", availableQty
                )
        );
        JsonNode data = response.path("data");
        return new TikTokRemoteStockView(
                reference.sku(),
                requiredInt(data, "available_qty", "Missing TikTok available_qty"),
                requiredInt(data, "reserved_qty", "Missing TikTok reserved_qty")
        );
    }

    private JsonNode execute(HttpMethod method, String path, Object payload) {
        long timestamp = timeProvider.now().getEpochSecond();
        String signature = TikTokSigningSupport.sign(
                method.name(),
                path,
                timestamp,
                properties.getAccessToken(),
                properties.getAppSecret()
        );
        String body;
        try {
            if (method == HttpMethod.GET) {
                body = restClient.get()
                        .uri(path)
                        .header("X-TTS-APP-KEY", properties.getAppKey())
                        .header("X-TTS-SHOP-CIPHER", properties.getShopCipher())
                        .header("X-TTS-ACCESS-TOKEN", properties.getAccessToken())
                        .header("X-TTS-TIMESTAMP", String.valueOf(timestamp))
                        .header("X-TTS-SIGNATURE", signature)
                        .retrieve()
                        .body(String.class);
            } else {
                body = restClient.post()
                        .uri(path)
                        .header("X-TTS-APP-KEY", properties.getAppKey())
                        .header("X-TTS-SHOP-CIPHER", properties.getShopCipher())
                        .header("X-TTS-ACCESS-TOKEN", properties.getAccessToken())
                        .header("X-TTS-TIMESTAMP", String.valueOf(timestamp))
                        .header("X-TTS-SIGNATURE", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            }
        } catch (ResourceAccessException exception) {
            throw new TransientChannelSyncException("TikTok connection failed: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new PermanentChannelSyncException("TikTok request configuration is invalid: " + exception.getMessage());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw new TransientChannelSyncException("TikTok server error: " + exception.getMessage());
            }
            throw new PermanentChannelSyncException("TikTok request rejected: " + exception.getMessage());
        } catch (RestClientException exception) {
            throw new TransientChannelSyncException("TikTok request failed: " + exception.getMessage());
        }
        return parse(body);
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            throw new TransientChannelSyncException("TikTok returned empty response");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").asInt(0);
            if (code != 0) {
                throw new PermanentChannelSyncException("TikTok rejected request: " + root.path("message").asText("unknown"));
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new PermanentChannelSyncException("TikTok returned invalid JSON payload");
        }
    }

    private int requiredInt(JsonNode node, String field, String message) {
        JsonNode value = node.path(field);
        if (!value.canConvertToInt()) {
            throw new PermanentChannelSyncException(message);
        }
        return value.asInt();
    }

    private String requiredText(JsonNode node, String field, String message) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new PermanentChannelSyncException(message);
        }
        return value.asText();
    }

    private void validateConfiguration() {
        requireNonBlank(properties.getBaseUrl(), "app.channel.tik-tok.base-url");
        requireNonBlank(properties.getAppKey(), "app.channel.tik-tok.app-key");
        requireNonBlank(properties.getAppSecret(), "app.channel.tik-tok.app-secret");
        requireNonBlank(properties.getShopCipher(), "app.channel.tik-tok.shop-cipher");
        requireNonBlank(properties.getAccessToken(), "app.channel.tik-tok.access-token");
        requireNonBlank(properties.getIngressSecret(), "app.channel.tik-tok.ingress-secret");
        requirePositiveDuration(properties.getConnectTimeout(), "app.channel.tik-tok.connect-timeout");
        requirePositiveDuration(properties.getReadTimeout(), "app.channel.tik-tok.read-timeout");
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required TikTok configuration: " + propertyName);
        }
    }

    private void requirePositiveDuration(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("TikTok timeout must be positive: " + propertyName);
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }
}
