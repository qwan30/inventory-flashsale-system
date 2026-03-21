package com.codex.flashsale.ai;

import com.codex.flashsale.api.OpsCopilotActionResponse;
import com.codex.flashsale.api.OpsCopilotAnalyzeRequest;
import com.codex.flashsale.api.OpsCopilotAnalyzeResponse;
import com.codex.flashsale.api.OpsCopilotCapabilitiesResponse;
import com.codex.flashsale.api.OpsCopilotCitationResponse;
import com.codex.flashsale.api.OpsCopilotFindingResponse;
import com.codex.flashsale.api.OpsCopilotProviderMetadataResponse;
import com.codex.flashsale.common.exception.BadRequestException;
import com.codex.flashsale.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class OpsCopilotService {

    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<OpsCopilotProvider> providerObjectProvider;
    private final OpsCopilotContextService contextService;
    private final OpsCopilotPromptFactory promptFactory;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer durationTimer;

    public OpsCopilotService(
            ApplicationProperties applicationProperties,
            ObjectProvider<OpsCopilotProvider> providerObjectProvider,
            OpsCopilotContextService contextService,
            OpsCopilotPromptFactory promptFactory,
            MeterRegistry meterRegistry
    ) {
        this.applicationProperties = applicationProperties;
        this.providerObjectProvider = providerObjectProvider;
        this.contextService = contextService;
        this.promptFactory = promptFactory;
        this.successCounter = meterRegistry.counter("ops.copilot.analysis.success");
        this.failureCounter = meterRegistry.counter("ops.copilot.analysis.failure");
        this.durationTimer = meterRegistry.timer("ops.copilot.analysis.duration");
    }

    public OpsCopilotCapabilitiesResponse getCapabilities() {
        Optional<OpsCopilotProvider> provider = resolveProvider();
        boolean enabled = applicationProperties.getAi().isEnabled()
                && provider.isPresent()
                && provider.get().isConfigured();

        return new OpsCopilotCapabilitiesResponse(
                enabled,
                true,
                provider.map(OpsCopilotProvider::providerName).orElse(normalizedProvider()),
                provider.map(OpsCopilotProvider::modelName).orElse(applicationProperties.getAi().getGemini().getModel()),
                OpsCopilotScope.supportedValues(),
                enabled ? "Ops copilot is ready for advisory analysis." : disabledReason(provider)
        );
    }

    public OpsCopilotAnalyzeResponse analyze(OpsCopilotAnalyzeRequest request) {
        OpsCopilotProvider provider = resolveProvider()
                .orElseThrow(() -> new BadRequestException("OPS_COPILOT_PROVIDER_UNAVAILABLE", "Ops copilot provider is unavailable"));
        if (!applicationProperties.getAi().isEnabled()) {
            throw new BadRequestException("OPS_COPILOT_DISABLED", "Ops copilot is disabled");
        }
        if (!provider.isConfigured()) {
            throw new BadRequestException("OPS_COPILOT_NOT_CONFIGURED", "Ops copilot provider credentials are not configured");
        }

        OpsCopilotScope scope = OpsCopilotScope.parse(request.scope());
        OpsCopilotContext context = contextService.buildContext(scope, request);
        String requestId = UUID.randomUUID().toString();

        Timer.Sample sample = Timer.start();
        try {
            OpsCopilotProviderResult providerResult = provider.analyze(
                    promptFactory.buildPrompt(context),
                    requestId
            );
            OpsCopilotAnalyzeResponse response = toResponse(context, provider, providerResult);
            successCounter.increment();
            return response;
        } catch (RuntimeException exception) {
            failureCounter.increment();
            throw exception;
        } finally {
            sample.stop(durationTimer);
        }
    }

    private OpsCopilotAnalyzeResponse toResponse(
            OpsCopilotContext context,
            OpsCopilotProvider provider,
            OpsCopilotProviderResult providerResult
    ) {
        OpsCopilotModelResponse modelResponse = providerResult.response();
        Map<String, OpsCopilotSource> sourceMap = new LinkedHashMap<>();
        context.sources().forEach(source -> sourceMap.put(source.sourceId(), source));
        List<String> allowedSourceIds = List.copyOf(sourceMap.keySet());

        List<OpsCopilotFindingResponse> findings = sanitizeFindings(modelResponse.prioritizedFindings(), allowedSourceIds);
        List<OpsCopilotActionResponse> actions = sanitizeActions(modelResponse.recommendedActions(), context.allowedHrefs(), allowedSourceIds, context);
        List<OpsCopilotCitationResponse> citations = collectCitations(findings, actions, sourceMap);

        return new OpsCopilotAnalyzeResponse(
                context.scope().name(),
                context.subjectId(),
                normalizeSummary(modelResponse.summary(), context.scope()),
                findings,
                actions,
                citations,
                new OpsCopilotProviderMetadataResponse(
                        provider.providerName(),
                        provider.modelName(),
                        true,
                        providerResult.requestId()
                )
        );
    }

    private List<OpsCopilotFindingResponse> sanitizeFindings(List<OpsCopilotModelFinding> findings, List<String> allowedSourceIds) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }
        return findings.stream()
                .limit(4)
                .map(finding -> new OpsCopilotFindingResponse(
                        normalizeSeverity(finding.severity()),
                        fallback(finding.title(), "Operational finding"),
                        fallback(finding.detail(), "No detail provided by the ops copilot."),
                        sanitizeSourceIds(finding.sourceIds(), allowedSourceIds)
                ))
                .toList();
    }

    private List<OpsCopilotActionResponse> sanitizeActions(
            List<OpsCopilotModelAction> actions,
            List<String> allowedHrefs,
            List<String> allowedSourceIds,
            OpsCopilotContext context
    ) {
        List<OpsCopilotActionResponse> sanitized = new ArrayList<>();
        if (actions != null) {
            actions.stream()
                    .limit(4)
                    .filter(action -> action.href() != null && allowedHrefs.contains(action.href()))
                    .map(action -> new OpsCopilotActionResponse(
                            fallback(action.label(), "Open workflow"),
                            action.href(),
                            fallback(action.rationale(), "Review the linked workflow for the current issue."),
                            sanitizeSourceIds(action.sourceIds(), allowedSourceIds)
                    ))
                    .forEach(sanitized::add);
        }
        if (!sanitized.isEmpty()) {
            return sanitized;
        }
        return defaultActions(context);
    }

    private List<OpsCopilotActionResponse> defaultActions(OpsCopilotContext context) {
        return switch (context.scope()) {
            case OPS_OVERVIEW -> List.of(
                    new OpsCopilotActionResponse("Open remediation", "/ops/remediation", "Review failed events, drifts, and retry options.", List.of()),
                    new OpsCopilotActionResponse("Open channel health", "/channels/health", "Inspect marketplace posture and drift signals.", List.of())
            );
            case BENCHMARK_RUN -> List.of(
                    new OpsCopilotActionResponse("Open benchmark detail", "/benchmarks/%s".formatted(context.subjectId()), "Inspect the benchmark evidence linked to this run.", List.of()),
                    new OpsCopilotActionResponse("Open benchmark list", "/benchmarks", "Compare this run against other promoted evidence.", List.of())
            );
            case CHANNEL_HEALTH -> List.of(
                    new OpsCopilotActionResponse("Open channel health", "/channels/health", "Inspect marketplace posture and operator signals.", List.of()),
                    new OpsCopilotActionResponse("Open remediation", "/ops/remediation", "Review manual remediation actions if the channel is degraded.", List.of())
            );
            case CAMPAIGN_AUDIT -> List.of(
                    new OpsCopilotActionResponse("Open campaign detail", "/campaigns/%s".formatted(context.subjectId()), "Review the campaign configuration and lifecycle state.", List.of()),
                    new OpsCopilotActionResponse("Open campaign audits", "/campaigns/%s/audits".formatted(context.subjectId()), "Inspect the recent campaign activity history.", List.of())
            );
        };
    }

    private List<OpsCopilotCitationResponse> collectCitations(
            List<OpsCopilotFindingResponse> findings,
            List<OpsCopilotActionResponse> actions,
            Map<String, OpsCopilotSource> sourceMap
    ) {
        LinkedHashSet<String> usedSourceIds = new LinkedHashSet<>();
        findings.forEach(finding -> usedSourceIds.addAll(finding.sourceIds()));
        actions.forEach(action -> usedSourceIds.addAll(action.sourceIds()));
        if (usedSourceIds.isEmpty()) {
            usedSourceIds.addAll(sourceMap.keySet());
        }
        return usedSourceIds.stream()
                .map(sourceMap::get)
                .filter(source -> source != null)
                .map(source -> new OpsCopilotCitationResponse(source.sourceId(), source.label(), source.detail()))
                .toList();
    }

    private List<String> sanitizeSourceIds(List<String> requestedSourceIds, List<String> allowedSourceIds) {
        if (requestedSourceIds == null || requestedSourceIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = requestedSourceIds.stream()
                .filter(allowedSourceIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(sanitized);
    }

    private Optional<OpsCopilotProvider> resolveProvider() {
        return Optional.ofNullable(providerObjectProvider.getIfAvailable());
    }

    private String disabledReason(Optional<OpsCopilotProvider> provider) {
        if (!applicationProperties.getAi().isEnabled()) {
            return "Ops copilot is disabled in configuration.";
        }
        if (provider.isEmpty()) {
            return "No ops copilot provider bean is available for the configured provider.";
        }
        if (!provider.get().isConfigured()) {
            return "Ops copilot provider credentials are missing or incomplete.";
        }
        return "Ops copilot is unavailable.";
    }

    private String normalizedProvider() {
        String provider = applicationProperties.getAi().getProvider();
        return provider == null || provider.isBlank() ? "unknown" : provider;
    }

    private String normalizeSummary(String summary, OpsCopilotScope scope) {
        return fallback(summary, switch (scope) {
            case OPS_OVERVIEW -> "No urgent operator guidance was generated. Review the remediation workflows directly.";
            case BENCHMARK_RUN -> "No benchmark-specific guidance was generated. Review the promoted evidence directly.";
            case CHANNEL_HEALTH -> "No channel-specific guidance was generated. Review the marketplace posture directly.";
            case CAMPAIGN_AUDIT -> "No campaign-audit guidance was generated. Review the audit trail directly.";
        });
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "INFO";
        }
        String normalized = severity.trim().toUpperCase();
        return switch (normalized) {
            case "WARN", "CRITICAL" -> normalized;
            default -> "INFO";
        };
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value;
    }
}
