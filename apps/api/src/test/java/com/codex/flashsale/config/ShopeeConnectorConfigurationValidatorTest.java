package com.codex.flashsale.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ShopeeConnectorConfigurationValidatorTest {

    @Test
    void shouldRejectMissingPartnerKeyForRealModeValidation() {
        ApplicationProperties properties = createValidShopeeProperties();
        properties.getChannel().getShopee().setPartnerKey(null);

        ShopeeConnectorConfigurationValidator validator = new ShopeeConnectorConfigurationValidator(properties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.channel.shopee.partner-key");
    }

    @Test
    void shouldAcceptFullySpecifiedRealModeConfiguration() {
        ApplicationProperties properties = createValidShopeeProperties();

        ShopeeConnectorConfigurationValidator validator = new ShopeeConnectorConfigurationValidator(properties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private ApplicationProperties createValidShopeeProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        ApplicationProperties.Shopee shopee = properties.getChannel().getShopee();
        shopee.setMode("real");
        shopee.setBaseUrl("https://partner.test-stable.shopeemobile.com");
        shopee.setPartnerId(123456L);
        shopee.setPartnerKey("partner-key");
        shopee.setShopId(456789L);
        shopee.setAccessToken("access-token");
        shopee.setConnectTimeout(Duration.ofSeconds(1));
        shopee.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }
}
