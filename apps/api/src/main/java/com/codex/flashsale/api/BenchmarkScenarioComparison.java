package com.codex.flashsale.api;

public record BenchmarkScenarioComparison(
        String scenarioName,
        boolean available,
        String note,
        BenchmarkScenarioDelta delta
) {
}
