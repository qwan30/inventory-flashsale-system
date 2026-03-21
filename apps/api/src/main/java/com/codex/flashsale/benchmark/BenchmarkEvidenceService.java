package com.codex.flashsale.benchmark;

import com.codex.flashsale.api.BenchmarkEvidenceDetailResponse;
import com.codex.flashsale.api.BenchmarkEvidenceEntryResponse;
import com.codex.flashsale.api.BenchmarkScenarioComparison;
import com.codex.flashsale.api.BenchmarkScenarioDelta;
import com.codex.flashsale.api.BenchmarkScenarioSummary;
import com.codex.flashsale.api.BenchmarkSuiteSummary;
import com.codex.flashsale.common.exception.NotFoundException;
import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkEvidenceService {

    private static final int MAX_SEARCH_DEPTH = 4;

    private final ObjectMapper objectMapper;
    private final String configuredEvidenceRoot;

    public BenchmarkEvidenceService(
            ObjectMapper objectMapper,
            ApplicationProperties applicationProperties
    ) {
        this.objectMapper = objectMapper;
        this.configuredEvidenceRoot = applicationProperties.getBenchmark().getEvidenceRoot();
    }

    public List<BenchmarkEvidenceEntryResponse> listEvidence() {
        JsonNode entries = loadIndex().path("entries");
        if (!entries.isArray()) {
            return List.of();
        }
        List<BenchmarkEvidenceEntryResponse> responses = new ArrayList<>();
        entries.forEach(entry -> responses.add(toEntry(entry)));
        responses.sort(Comparator.comparing(BenchmarkEvidenceEntryResponse::timestamp).reversed());
        return responses;
    }

    public BenchmarkEvidenceDetailResponse getEvidence(String runId) {
        JsonNode entryNode = findEntryNode(runId);
        BenchmarkEvidenceEntryResponse entry = toEntry(entryNode);
        JsonNode manifest = readJson(resolvePath(entryNode, "manifestPath", "manifest.json"));
        JsonNode report = readJson(resolvePath(entryNode, "reportPath", "report.json"));
        JsonNode comparison = readOptionalJson(resolveOptionalPath(entryNode, "comparisonPath", "comparison.json"));
        String summaryMarkdown = readOptionalText(resolveOptionalPath(entryNode, "summaryPath", "summary.md"));
        return new BenchmarkEvidenceDetailResponse(
                entry,
                manifest,
                report,
                comparison,
                summaryMarkdown,
                toSuiteSummary(report, comparison),
                toScenarioSummaries(report),
                toScenarioComparisons(report, comparison)
        );
    }

    public BenchmarkEvidenceDetailResponse getLatestEvidence() {
        return listEvidence().stream()
                .findFirst()
                .map(entry -> getEvidence(entry.runId()))
                .orElseThrow(() -> new NotFoundException("BENCHMARK_EVIDENCE_NOT_FOUND", "No promoted benchmark evidence found"));
    }

    private JsonNode findEntryNode(String runId) {
        JsonNode entries = loadIndex().path("entries");
        if (entries.isArray()) {
            for (JsonNode entry : entries) {
                if (runId.equals(buildRunId(entry))) {
                    return entry;
                }
            }
        }
        throw new NotFoundException("BENCHMARK_EVIDENCE_NOT_FOUND", "Benchmark evidence run not found: " + runId);
    }

    private JsonNode loadIndex() {
        Path indexPath = resolveEvidenceRoot().resolve("index.json");
        if (!Files.exists(indexPath)) {
            throw new NotFoundException("BENCHMARK_EVIDENCE_NOT_FOUND", "Benchmark evidence index not found: " + indexPath);
        }
        return readJson(indexPath);
    }

    private BenchmarkEvidenceEntryResponse toEntry(JsonNode entry) {
        return new BenchmarkEvidenceEntryResponse(
                buildRunId(entry),
                text(entry, "timestamp"),
                text(entry, "gitCommit"),
                text(entry, "evidenceDir"),
                text(entry, "suiteStatus"),
                entry.path("businessChecksPassed").asBoolean(false),
                text(entry, "baselineTarget")
        );
    }

    private String buildRunId(JsonNode entry) {
        String evidenceDir = text(entry, "evidenceDir");
        if (evidenceDir != null && !evidenceDir.isBlank()) {
            return evidenceDir;
        }
        return text(entry, "timestamp") + "-" + text(entry, "gitCommit");
    }

    private Path resolvePath(JsonNode entry, String fieldName, String fallbackFileName) {
        Path resolved = resolveOptionalPath(entry, fieldName, fallbackFileName);
        if (resolved != null) {
            return resolved;
        }
        throw new NotFoundException("BENCHMARK_EVIDENCE_FILE_NOT_FOUND", "Benchmark evidence file not found for " + buildRunId(entry));
    }

    private Path resolveOptionalPath(JsonNode entry, String fieldName, String fallbackFileName) {
        String configured = text(entry, fieldName);
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured);
            if (Files.exists(path)) {
                return path;
            }
        }
        String evidenceDir = text(entry, "evidenceDir");
        if (evidenceDir == null || evidenceDir.isBlank()) {
            throw new NotFoundException("BENCHMARK_EVIDENCE_FILE_NOT_FOUND", "Missing evidence directory for run " + buildRunId(entry));
        }
        Path fallback = resolveEvidenceRoot().resolve(evidenceDir).resolve(fallbackFileName);
        if (Files.exists(fallback)) {
            return fallback;
        }
        return null;
    }

    private Path resolveEvidenceRoot() {
        Path configuredPath = Path.of(configuredEvidenceRoot);
        if (Files.exists(configuredPath)) {
            return configuredPath;
        }

        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < MAX_SEARCH_DEPTH; depth++) {
            Path candidate = current.resolve(configuredEvidenceRoot);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = Optional.ofNullable(current.getParent()).orElse(current);
        }
        return configuredPath;
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read benchmark evidence JSON: " + path, exception);
        }
    }

    private String readText(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read benchmark evidence summary: " + path, exception);
        }
    }

    private JsonNode readOptionalJson(Path path) {
        return path == null ? NullNode.instance : readJson(path);
    }

    private String readOptionalText(Path path) {
        return path == null ? null : readText(path);
    }

    private BenchmarkSuiteSummary toSuiteSummary(JsonNode report, JsonNode comparison) {
        if (report == null || report.isMissingNode()) {
            return new BenchmarkSuiteSummary(null, false, null, false, null);
        }
        JsonNode businessChecks = report.path("businessChecks");
        JsonNode baselineComparison = baselineComparisonNode(report, comparison);
        return new BenchmarkSuiteSummary(
                text(report, "suiteStatus"),
                businessChecks.path("passed").asBoolean(false),
                text(baselineComparison, "baselineTarget"),
                baselineComparison.path("available").asBoolean(false),
                text(baselineComparison, "note")
        );
    }

    private List<BenchmarkScenarioSummary> toScenarioSummaries(JsonNode report) {
        JsonNode scenarios = report.path("scenarioResults");
        if (!scenarios.isArray()) {
            return Collections.emptyList();
        }
        List<BenchmarkScenarioSummary> summaries = new ArrayList<>();
        for (JsonNode scenario : scenarios) {
            JsonNode stats = scenario.path("stats");
            List<String> postRunChecks = toStringList(scenario.path("postRunChecks"));
            summaries.add(new BenchmarkScenarioSummary(
                    text(scenario, "name"),
                    text(scenario, "status"),
                    nullableDouble(stats, "httpReqDurationAvg"),
                    nullableDouble(stats, "httpReqDurationP95"),
                    nullableDouble(stats, "httpReqFailedRate"),
                    nullableDouble(stats, "checksRate"),
                    postRunChecks
            ));
        }
        return summaries;
    }

    private List<BenchmarkScenarioComparison> toScenarioComparisons(JsonNode report, JsonNode comparison) {
        JsonNode baselineComparison = baselineComparisonNode(report, comparison);
        JsonNode deltas = baselineComparison.path("scenarioDeltas");
        if (!deltas.isArray()) {
            return Collections.emptyList();
        }
        List<BenchmarkScenarioComparison> comparisons = new ArrayList<>();
        for (JsonNode delta : deltas) {
            comparisons.add(toScenarioComparison(delta));
        }
        return comparisons;
    }

    private JsonNode baselineComparisonNode(JsonNode report, JsonNode comparison) {
        if (comparison != null && !comparison.isMissingNode() && !comparison.isNull()) {
            return comparison;
        }
        return report.path("baselineComparison");
    }

    private BenchmarkScenarioComparison toScenarioComparison(JsonNode delta) {
        JsonNode deltaValues = delta.path("deltas");
        return new BenchmarkScenarioComparison(
                text(delta, "name"),
                delta.path("available").asBoolean(false),
                text(delta, "note"),
                toScenarioDelta(deltaValues)
        );
    }

    private BenchmarkScenarioDelta toScenarioDelta(JsonNode deltas) {
        if (deltas == null || deltas.isMissingNode()) {
            return new BenchmarkScenarioDelta(null, null, null, null);
        }
        return new BenchmarkScenarioDelta(
                nullableDouble(deltas, "httpReqDurationAvg"),
                nullableDouble(deltas, "httpReqDurationP95"),
                nullableDouble(deltas, "httpReqFailedRate"),
                nullableDouble(deltas, "checksRate")
        );
    }

    private List<String> toStringList(JsonNode node) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode element : node) {
            String text = element.asText(null);
            if (text != null) {
                values.add(text);
            }
        }
        return values;
    }

    private Double nullableDouble(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        if (!value.isNumber()) {
            return null;
        }
        return value.doubleValue();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
