package com.codex.flashsale.api;

public record BenchmarkSuiteSummary(
        String suiteStatus,
        boolean businessChecksPassed,
        String baselineTarget,
        boolean baselineAvailable,
        String baselineNote
) {
}
