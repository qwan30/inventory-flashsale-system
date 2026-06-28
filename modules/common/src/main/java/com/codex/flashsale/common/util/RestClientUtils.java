package com.codex.flashsale.common.util;

import java.time.Duration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public final class RestClientUtils {

    private RestClientUtils() {
    }

    public static ClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (connectTimeout != null) {
            factory.setConnectTimeout((int) connectTimeout.toMillis());
        }
        if (readTimeout != null) {
            factory.setReadTimeout((int) readTimeout.toMillis());
        }
        return factory;
    }
}
