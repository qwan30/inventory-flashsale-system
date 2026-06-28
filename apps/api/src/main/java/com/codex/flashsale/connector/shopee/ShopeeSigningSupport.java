package com.codex.flashsale.connector.shopee;

import com.codex.flashsale.common.util.HexUtils;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class ShopeeSigningSupport {

    private ShopeeSigningSupport() {
    }

    static String signShopRequest(
            long partnerId,
            String apiPath,
            long timestamp,
            String accessToken,
            long shopId,
            String partnerKey
    ) {
        String baseString = partnerId + apiPath + timestamp + accessToken + shopId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return HexUtils.toHexString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate Shopee request signature", exception);
        }
    }
}
