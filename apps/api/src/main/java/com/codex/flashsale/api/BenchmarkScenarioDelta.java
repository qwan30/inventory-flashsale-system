package com.codex.flashsale.api;

public record BenchmarkScenarioDelta(
        Double deltaAverageLatencyMs,
        Double deltaP95LatencyMs,
        Double deltaFailedRate,
        Double deltaChecksRate
) {
}
