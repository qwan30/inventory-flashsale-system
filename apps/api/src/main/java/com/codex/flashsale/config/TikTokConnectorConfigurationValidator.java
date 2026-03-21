package com.codex.flashsale.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "real")
public class TikTokConnectorConfigurationValidator {

    private final ApplicationProperties.TikTok tikTokProperties;

    public TikTokConnectorConfigurationValidator(ApplicationProperties applicationProperties) {
        this.tikTokProperties = applicationProperties.getChannel().getTikTok();
    }

    @PostConstruct
    void validate() {
        requireNonBlank(tikTokProperties.getBaseUrl(), "app.channel.tik-tok.base-url");
        requireNonBlank(tikTokProperties.getAppKey(), "app.channel.tik-tok.app-key");
        requireNonBlank(tikTokProperties.getAppSecret(), "app.channel.tik-tok.app-secret");
        requireNonBlank(tikTokProperties.getShopCipher(), "app.channel.tik-tok.shop-cipher");
        requireNonBlank(tikTokProperties.getAccessToken(), "app.channel.tik-tok.access-token");
        requireNonBlank(tikTokProperties.getIngressSecret(), "app.channel.tik-tok.ingress-secret");
        requirePositiveDuration(tikTokProperties.getConnectTimeout(), "app.channel.tik-tok.connect-timeout");
        requirePositiveDuration(tikTokProperties.getReadTimeout(), "app.channel.tik-tok.read-timeout");
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required TikTok configuration: " + propertyName);
        }
    }

    private void requirePositiveDuration(java.time.Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("TikTok timeout must be positive: " + propertyName);
        }
    }
}
