package com.codex.flashsale.api;

public record BenchmarkEvidenceEntryResponse(
        String runId,
        String timestamp,
        String gitCommit,
        String evidenceDir,
        String suiteStatus,
        boolean businessChecksPassed,
        String baselineTarget
) {
}
