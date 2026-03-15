package com.codex.flashsale.connector.shopee;

import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import java.util.Locale;
import java.util.Set;

final class ShopeeErrorClassifier {

    private static final Set<String> TRANSIENT_ERROR_CODES = Set.of(
            "error_server",
            "error_inner",
            "error_network",
            "error_system_busy"
    );

    private ShopeeErrorClassifier() {
    }

    static RuntimeException toChannelSyncException(String errorCode, String message) {
        String normalizedError = normalize(errorCode);
        String normalizedMessage = (message == null || message.isBlank())
                ? "Shopee API error"
                : message;
        String fullMessage = normalizedError == null
                ? normalizedMessage
                : normalizedError + ": " + normalizedMessage;
        if (normalizedError != null && TRANSIENT_ERROR_CODES.contains(normalizedError)) {
            return new TransientChannelSyncException(fullMessage);
        }
        return new PermanentChannelSyncException(fullMessage);
    }

    private static String normalize(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }
        String trimmed = errorCode.trim();
        int index = trimmed.lastIndexOf('.');
        String normalized = index >= 0 ? trimmed.substring(index + 1) : trimmed;
        return normalized.toLowerCase(Locale.ROOT);
    }
}
