package com.codex.flashsale.controller;

import com.codex.flashsale.api.BenchmarkEvidenceDetailResponse;
import com.codex.flashsale.api.BenchmarkEvidenceEntryResponse;
import com.codex.flashsale.benchmark.BenchmarkEvidenceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ops/benchmarks/evidence")
public class AdminBenchmarkController {

    private final BenchmarkEvidenceService benchmarkEvidenceService;

    public AdminBenchmarkController(BenchmarkEvidenceService benchmarkEvidenceService) {
        this.benchmarkEvidenceService = benchmarkEvidenceService;
    }

    @GetMapping
    public List<BenchmarkEvidenceEntryResponse> listEvidence() {
        return benchmarkEvidenceService.listEvidence();
    }

    @GetMapping("/latest")
    public BenchmarkEvidenceDetailResponse latestEvidence() {
        return benchmarkEvidenceService.getLatestEvidence();
    }

    @GetMapping("/{runId}")
    public BenchmarkEvidenceDetailResponse evidence(@PathVariable String runId) {
        return benchmarkEvidenceService.getEvidence(runId);
    }
}
