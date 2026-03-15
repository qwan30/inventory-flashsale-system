package com.codex.flashsale.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "real")
public class ShopeeConnectorConfigurationValidator {

    private final ApplicationProperties.Shopee shopeeProperties;

    public ShopeeConnectorConfigurationValidator(ApplicationProperties applicationProperties) {
        this.shopeeProperties = applicationProperties.getChannel().getShopee();
    }

    @PostConstruct
    void validate() {
        requireNonBlank(shopeeProperties.getBaseUrl(), "app.channel.shopee.base-url");
        requireNonNull(shopeeProperties.getPartnerId(), "app.channel.shopee.partner-id");
        requireNonBlank(shopeeProperties.getPartnerKey(), "app.channel.shopee.partner-key");
        requireNonNull(shopeeProperties.getShopId(), "app.channel.shopee.shop-id");
        requireNonBlank(shopeeProperties.getAccessToken(), "app.channel.shopee.access-token");
        requirePositiveDuration(shopeeProperties.getConnectTimeout(), "app.channel.shopee.connect-timeout");
        requirePositiveDuration(shopeeProperties.getReadTimeout(), "app.channel.shopee.read-timeout");
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

    private void requirePositiveDuration(java.time.Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("Shopee timeout must be positive: " + propertyName);
        }
    }
}
