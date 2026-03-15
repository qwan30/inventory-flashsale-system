package com.codex.flashsale.alerts;

import com.codex.flashsale.api.OpsAlertSeverity;
import com.codex.flashsale.api.OpsAlertStatus;
import java.time.Instant;

public record AlertDeliveryPayload(
        String source,
        String code,
        OpsAlertSeverity severity,
        OpsAlertStatus status,
        String message,
        String currentValue,
        String threshold,
        Instant observedAt,
        AlertDispatchType dispatchType,
        Instant dispatchedAt
) {
}
