package com.codex.flashsale.admin;

import com.codex.flashsale.common.http.HeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

public record AdminRequestMetadata(String correlationId, String remoteAddress, String userAgent) {

    public static AdminRequestMetadata from(HttpServletRequest request) {
        String correlationId = MDC.get(HeaderNames.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(HeaderNames.CORRELATION_ID);
        }
        return new AdminRequestMetadata(correlationId, request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public String asDetail() {
        return "ip=%s, userAgent=%s".formatted(remoteAddress, userAgent == null ? "unknown" : userAgent);
    }
}
