package com.codex.flashsale.connector.shopee;

import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "real")
public class ShopeeRestChannelClient implements ShopeeChannelClient {

    private static final int MAX_ITEM_SCAN = 100;
    private static final String GET_ITEM_LIST_PATH = "/api/v2/product/get_item_list";
    private static final String GET_ITEM_BASE_INFO_PATH = "/api/v2/product/get_item_base_info";
    private static final String GET_MODEL_LIST_PATH = "/api/v2/product/get_model_list";
    private static final String UPDATE_STOCK_PATH = "/api/v2/product/update_stock";
    private static final String BASE_URL_PROPERTY = "app.channel.shopee.base-url";
    private static final String PARTNER_ID_PROPERTY = "app.channel.shopee.partner-id";
    private static final String PARTNER_KEY_PROPERTY = "app.channel.shopee.partner-key";
    private static final String SHOP_ID_PROPERTY = "app.channel.shopee.shop-id";
    private static final String ACCESS_TOKEN_PROPERTY = "app.channel.shopee.access-token";
    private static final String CONNECT_TIMEOUT_PROPERTY = "app.channel.shopee.connect-timeout";
    private static final String READ_TIMEOUT_PROPERTY = "app.channel.shopee.read-timeout";

    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;
    private final RestClient restClient;
    private final ApplicationProperties.Shopee properties;

    public ShopeeRestChannelClient(
            ObjectMapper objectMapper,
            TimeProvider timeProvider,
            ApplicationProperties applicationProperties
    ) {
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
        this.properties = applicationProperties.getChannel().getShopee();
        validateShopeeConfiguration(this.properties);
        this.restClient = RestClient.builder()
                .baseUrl(this.properties.getBaseUrl())
                .requestFactory(createRequestFactory(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
                .build();
    }

    @Override
    public Optional<ShopeeListingView> findListingBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        String normalizedSku = sku.trim();
        List<ShopeeListingView> matches = new ArrayList<>();
        for (Long itemId : fetchItemIds()) {
            JsonNode item = getItemBaseInfo(itemId);
            boolean hasModel = item.path("has_model").asBoolean(false);
            String itemSku = text(item, "item_sku");
            if (!hasModel && normalizedSku.equals(itemSku)) {
                matches.add(new ShopeeListingView(
                        toItemReference(item, normalizedSku),
                        toItemStockView(item, normalizedSku)
                ));
                continue;
            }
            if (hasModel) {
                JsonNode models = getModelList(itemId);
                for (JsonNode model : models) {
                    if (normalizedSku.equals(text(model, "model_sku"))) {
                        matches.add(new ShopeeListingView(
                                toModelReference(item, model, normalizedSku),
                                toModelStockView(model, normalizedSku)
                        ));
                    }
                }
            }
        }
        if (matches.size() > 1) {
            throw new PermanentChannelSyncException("Ambiguous Shopee SKU mapping for " + normalizedSku);
        }
        return matches.stream().findFirst();
    }

    @Override
    public ShopeeRemoteStockView updateSellerStock(ShopeeListingReference reference, int sellerStock) {
        if (reference == null) {
            throw new PermanentChannelSyncException("Shopee listing reference is required");
        }
        if (sellerStock < 0) {
            throw new PermanentChannelSyncException("Shopee stock cannot be negative");
        }
        JsonNode response = execute(
                HttpMethod.POST,
                UPDATE_STOCK_PATH,
                Map.of(),
                Map.of(
                        "item_id", reference.itemId(),
                        "stock_list", List.of(Map.of(
                                "model_id", reference.modelId() == null ? 0L : reference.modelId(),
                                "seller_stock", List.of(sellerStockPayload(reference, sellerStock))
                        ))
                )
        );
        JsonNode failureList = response.path("response").path("failure_list");
        if (failureList.isArray() && !failureList.isEmpty()) {
            String failedReason = text(failureList.get(0), "failed_reason");
            throw new PermanentChannelSyncException("Shopee update_stock failure: " + failedReason);
        }
        return fetchStockForReference(reference);
    }

    private List<Long> fetchItemIds() {
        JsonNode response = execute(
                HttpMethod.GET,
                GET_ITEM_LIST_PATH,
                Map.of(
                        "offset", "0",
                        "page_size", String.valueOf(MAX_ITEM_SCAN),
                        "item_status", "NORMAL"
                ),
                null
        );
        List<Long> itemIds = new ArrayList<>();
        JsonNode items = response.path("response").path("item");
        if (!items.isArray()) {
            return itemIds;
        }
        for (JsonNode item : items) {
            itemIds.add(asLongRequired(item, "item_id", "Missing Shopee item_id"));
        }
        return itemIds;
    }

    private JsonNode getItemBaseInfo(long itemId) {
        JsonNode response = execute(
                HttpMethod.GET,
                GET_ITEM_BASE_INFO_PATH,
                Map.of("item_id_list", "[" + itemId + "]"),
                null
        );
        JsonNode itemList = response.path("response").path("item_list");
        if (!itemList.isArray() || itemList.isEmpty()) {
            throw new PermanentChannelSyncException("Shopee item not found for item_id " + itemId);
        }
        return itemList.get(0);
    }

    private JsonNode getModelList(long itemId) {
        JsonNode response = execute(
                HttpMethod.GET,
                GET_MODEL_LIST_PATH,
                Map.of("item_id", String.valueOf(itemId)),
                null
        );
        JsonNode models = response.path("response").path("model");
        if (!models.isArray()) {
            throw new PermanentChannelSyncException("Shopee get_model_list returned unsupported payload");
        }
        return models;
    }

    private ShopeeRemoteStockView fetchStockForReference(ShopeeListingReference reference) {
        JsonNode item = getItemBaseInfo(reference.itemId());
        if (reference.modelId() == null) {
            return toItemStockView(item, reference.sku());
        }
        JsonNode models = getModelList(reference.itemId());
        for (JsonNode model : models) {
            if (asLongRequired(model, "model_id", "Missing Shopee model_id") == reference.modelId()) {
                return toModelStockView(model, reference.sku());
            }
        }
        throw new PermanentChannelSyncException("Shopee model not found for model_id " + reference.modelId());
    }

    private ShopeeListingReference toItemReference(JsonNode item, String sku) {
        JsonNode sellerStock = requireSingleSellerStockNode(item.path("stock_info_v2").path("seller_stock"), sku);
        return new ShopeeListingReference(
                sku,
                asLongRequired(item, "item_id", "Missing Shopee item_id"),
                null,
                text(sellerStock, "location_id")
        );
    }

    private ShopeeListingReference toModelReference(JsonNode item, JsonNode model, String sku) {
        JsonNode sellerStock = requireSingleSellerStockNode(model.path("stock_info_v2").path("seller_stock"), sku);
        return new ShopeeListingReference(
                sku,
                asLongRequired(item, "item_id", "Missing Shopee item_id"),
                asLongRequired(model, "model_id", "Missing Shopee model_id"),
                text(sellerStock, "location_id")
        );
    }

    private ShopeeRemoteStockView toItemStockView(JsonNode item, String sku) {
        JsonNode summary = item.path("stock_info_v2").path("summary_info");
        return new ShopeeRemoteStockView(
                sku,
                asIntRequired(summary, "total_available_stock", "Missing Shopee available stock"),
                asIntRequired(summary, "total_reserved_stock", "Missing Shopee reserved stock")
        );
    }

    private ShopeeRemoteStockView toModelStockView(JsonNode model, String sku) {
        JsonNode summary = model.path("stock_info_v2").path("summary_info");
        return new ShopeeRemoteStockView(
                sku,
                asIntRequired(summary, "total_available_stock", "Missing Shopee model available stock"),
                asIntRequired(summary, "total_reserved_stock", "Missing Shopee model reserved stock")
        );
    }

    private JsonNode requireSingleSellerStockNode(JsonNode sellerStockArray, String sku) {
        if (!sellerStockArray.isArray() || sellerStockArray.size() != 1) {
            throw new PermanentChannelSyncException(
                    "Unsupported Shopee stock structure for " + sku + ". Expected exactly one seller_stock location."
            );
        }
        JsonNode sellerStock = sellerStockArray.get(0);
        if (!sellerStock.path("stock").canConvertToInt()) {
            throw new PermanentChannelSyncException("Unsupported Shopee stock payload for " + sku);
        }
        return sellerStock;
    }

    private JsonNode execute(HttpMethod method, String path, Map<String, String> additionalParams, Object payload) {
        long timestamp = timeProvider.now().getEpochSecond();
        String sign = ShopeeSigningSupport.signShopRequest(
                properties.getPartnerId(),
                path,
                timestamp,
                properties.getAccessToken(),
                properties.getShopId(),
                properties.getPartnerKey()
        );

        String body;
        try {
            if (method == HttpMethod.GET) {
                body = restClient.get()
                        .uri(uriBuilder -> withQueryParams(
                                uriBuilder.path(path),
                                timestamp,
                                sign,
                                additionalParams
                        ).build())
                        .retrieve()
                        .body(String.class);
            } else {
                body = restClient.post()
                        .uri(uriBuilder -> withQueryParams(
                                uriBuilder.path(path),
                                timestamp,
                                sign,
                                additionalParams
                        ).build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            }
        } catch (ResourceAccessException exception) {
            throw new TransientChannelSyncException("Shopee connection failed: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new PermanentChannelSyncException("Shopee request configuration is invalid: " + exception.getMessage());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw new TransientChannelSyncException("Shopee server error: " + exception.getMessage());
            }
            throw new PermanentChannelSyncException("Shopee request rejected: " + exception.getMessage());
        } catch (RestClientException exception) {
            throw new TransientChannelSyncException("Shopee request failed: " + exception.getMessage());
        }

        JsonNode root = parseResponse(body);
        String errorCode = text(root, "error");
        if (errorCode != null && !errorCode.isBlank() && !"-".equals(errorCode)) {
            throw ShopeeErrorClassifier.toChannelSyncException(errorCode, text(root, "message"));
        }
        return root;
    }

    private org.springframework.web.util.UriBuilder withQueryParams(
            org.springframework.web.util.UriBuilder builder,
            long timestamp,
            String sign,
            Map<String, String> additionalParams
    ) {
        builder.queryParam("partner_id", properties.getPartnerId());
        builder.queryParam("timestamp", timestamp);
        builder.queryParam("access_token", properties.getAccessToken());
        builder.queryParam("shop_id", properties.getShopId());
        builder.queryParam("sign", sign);
        additionalParams.forEach(builder::queryParam);
        return builder;
    }

    private JsonNode parseResponse(String body) {
        if (body == null || body.isBlank()) {
            throw new TransientChannelSyncException("Shopee returned empty response");
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            throw new PermanentChannelSyncException("Shopee returned invalid JSON payload");
        }
    }

    private long asLongRequired(JsonNode node, String field, String message) {
        JsonNode value = node.path(field);
        if (!value.canConvertToLong()) {
            throw new PermanentChannelSyncException(message);
        }
        return value.asLong();
    }

    private int asIntRequired(JsonNode node, String field, String message) {
        JsonNode value = node.path(field);
        if (!value.canConvertToInt()) {
            throw new PermanentChannelSyncException(message);
        }
        return value.asInt();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Map<String, Object> sellerStockPayload(ShopeeListingReference reference, int sellerStock) {
        if (reference.locationId() == null || reference.locationId().isBlank()) {
            return Map.of("stock", sellerStock);
        }
        return Map.of(
                "location_id", reference.locationId(),
                "stock", sellerStock
        );
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }

    private void validateShopeeConfiguration(ApplicationProperties.Shopee shopee) {
        requireNonBlank(shopee.getBaseUrl(), BASE_URL_PROPERTY);
        requireNonNull(shopee.getPartnerId(), PARTNER_ID_PROPERTY);
        requireNonBlank(shopee.getPartnerKey(), PARTNER_KEY_PROPERTY);
        requireNonNull(shopee.getShopId(), SHOP_ID_PROPERTY);
        requireNonBlank(shopee.getAccessToken(), ACCESS_TOKEN_PROPERTY);
        requirePositiveDuration(shopee.getConnectTimeout(), CONNECT_TIMEOUT_PROPERTY);
        requirePositiveDuration(shopee.getReadTimeout(), READ_TIMEOUT_PROPERTY);
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required Shopee configuration: " + propertyName);
        }
    }

    private void requireNonNull(Object value, String propertyName) {
        if (value == null) {
            throw new IllegalStateException("Missing required Shopee configuration: " + propertyName);
        }
    }

    private void requirePositiveDuration(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("Shopee timeout must be positive: " + propertyName);
        }
    }
}
