package com.codex.flashsale.connector.shopee;

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
            return toLowerHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate Shopee request signature", exception);
        }
    }

    private static String toLowerHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >>> 4) & 0x0F, 16));
            builder.append(Character.forDigit(value & 0x0F, 16));
        }
        return builder.toString();
    }
}
