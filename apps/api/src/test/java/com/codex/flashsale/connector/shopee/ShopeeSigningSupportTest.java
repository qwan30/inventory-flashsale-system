package com.codex.flashsale.connector.shopee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShopeeSigningSupportTest {

    @Test
    void shouldGenerateDeterministicShopRequestSignature() {
        String signature = ShopeeSigningSupport.signShopRequest(
                123456L,
                "/api/v2/product/update_stock",
                1700000000L,
                "access-token",
                456789L,
                "secret-key"
        );

        assertThat(signature).isEqualTo("3bca49efad40556e69f380ce3c58fa6998f48622caed9752a9c0b57e35bf6607");
    }
}
