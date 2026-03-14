package com.codex.flashsale.config;

import com.codex.flashsale.common.http.HeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = Optional.ofNullable(request.getHeader(HeaderNames.CORRELATION_ID))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        response.setHeader(HeaderNames.CORRELATION_ID, correlationId);
        MDC.put(HeaderNames.CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(HeaderNames.CORRELATION_ID);
        }
    }
}
