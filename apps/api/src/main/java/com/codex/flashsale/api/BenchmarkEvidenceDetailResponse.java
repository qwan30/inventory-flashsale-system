package com.codex.flashsale.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record BenchmarkEvidenceDetailResponse(
        BenchmarkEvidenceEntryResponse entry,
        JsonNode manifest,
        JsonNode report,
        JsonNode comparison,
        String summaryMarkdown,
        BenchmarkSuiteSummary suiteSummary,
        List<BenchmarkScenarioSummary> scenarioSummaries,
        List<BenchmarkScenarioComparison> scenarioComparisons
) {
}
