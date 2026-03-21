package com.codex.flashsale.ai;

import com.codex.flashsale.common.exception.BadRequestException;
import java.util.List;
import java.util.Locale;

public enum OpsCopilotScope {
    OPS_OVERVIEW,
    BENCHMARK_RUN,
    CHANNEL_HEALTH,
    CAMPAIGN_AUDIT;

    public static OpsCopilotScope parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("OPS_COPILOT_SCOPE_REQUIRED", "Ops copilot scope is required");
        }
        try {
            return OpsCopilotScope.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "OPS_COPILOT_SCOPE_UNSUPPORTED",
                    "Unsupported ops copilot scope: " + rawValue
            );
        }
    }

    public static List<String> supportedValues() {
        return List.of(
                OPS_OVERVIEW.name(),
                BENCHMARK_RUN.name(),
                CHANNEL_HEALTH.name(),
                CAMPAIGN_AUDIT.name()
        );
    }
}
