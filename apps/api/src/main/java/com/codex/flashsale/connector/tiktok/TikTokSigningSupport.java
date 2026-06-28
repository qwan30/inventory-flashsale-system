package com.codex.flashsale.connector.tiktok;

import com.codex.flashsale.common.util.HexUtils;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TikTokSigningSupport {

    private TikTokSigningSupport() {
    }

    public static String sign(String method, String path, long timestamp, String accessToken, String appSecret) {
        String baseString = method + "\n" + path + "\n" + timestamp + "\n" + accessToken;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexUtils.toHexString(mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate TikTok request signature", exception);
        }
    }

    public static String hashRawBody(String secret, String timestamp, String rawBody) {
        String baseString = timestamp + "." + rawBody;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexUtils.toHexString(mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate TikTok ingress signature", exception);
        }
    }
}
