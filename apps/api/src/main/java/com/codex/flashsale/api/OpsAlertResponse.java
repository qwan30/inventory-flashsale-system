package com.codex.flashsale.api;

import java.time.Instant;

public record OpsAlertResponse(
        String code,
        OpsAlertSeverity severity,
        OpsAlertStatus status,
        String message,
        String currentValue,
        String threshold,
        Instant observedAt
) {
}
