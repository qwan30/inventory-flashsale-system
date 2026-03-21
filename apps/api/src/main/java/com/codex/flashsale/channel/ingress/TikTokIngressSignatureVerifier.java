package com.codex.flashsale.channel.ingress;

import com.codex.flashsale.common.exception.UnauthorizedException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import com.codex.flashsale.connector.tiktok.TikTokSigningSupport;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TikTokIngressSignatureVerifier {

    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final String ingressSecret;
    private final TimeProvider timeProvider;

    public TikTokIngressSignatureVerifier(
            ApplicationProperties applicationProperties,
            TimeProvider timeProvider
    ) {
        this.ingressSecret = applicationProperties.getChannel().getTikTok().getIngressSecret();
        this.timeProvider = timeProvider;
    }

    public void verify(String rawBody, String timestampHeader, String signatureHeader) {
        if (timestampHeader == null || timestampHeader.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            throw new UnauthorizedException("TIKTOK_INGRESS_UNAUTHORIZED", "Missing TikTok ingress signature headers");
        }
        Instant requestTime;
        try {
            requestTime = Instant.ofEpochSecond(Long.parseLong(timestampHeader));
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("TIKTOK_INGRESS_UNAUTHORIZED", "Invalid TikTok ingress timestamp");
        }
        Instant now = timeProvider.now();
        if (requestTime.isBefore(now.minus(MAX_CLOCK_SKEW)) || requestTime.isAfter(now.plus(MAX_CLOCK_SKEW))) {
            throw new UnauthorizedException("TIKTOK_INGRESS_UNAUTHORIZED", "TikTok ingress request timestamp is outside the allowed skew");
        }
        String expectedSignature = TikTokSigningSupport.hashRawBody(ingressSecret, timestampHeader, rawBody);
        if (!expectedSignature.equals(signatureHeader)) {
            throw new UnauthorizedException("TIKTOK_INGRESS_UNAUTHORIZED", "Invalid TikTok ingress signature");
        }
    }
}
