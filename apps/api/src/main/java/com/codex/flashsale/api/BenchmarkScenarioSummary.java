package com.codex.flashsale.api;

import java.util.List;

public record BenchmarkScenarioSummary(
        String name,
        String status,
        Double averageLatencyMs,
        Double p95LatencyMs,
        Double failedRate,
        Double checksRate,
        List<String> postRunChecks
) {
}
