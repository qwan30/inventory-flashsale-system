package com.codex.flashsale.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codex.flashsale.api.OpsCopilotAnalyzeRequest;
import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class OpsCopilotServiceTest {

    @Test
    void shouldFallbackToSafeActionsAndCitationsWhenProviderReturnsUnsafeOutput() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getAi().setEnabled(true);

        OpsCopilotProvider provider = new StubProvider(
                new OpsCopilotProviderResult(
                        "req-1",
                        new OpsCopilotModelResponse(
                                "Operator review recommended.",
                                List.of(new OpsCopilotModelFinding("warn", "Backlog", "Failed events are accumulating.", List.of("alerts-current", "unknown"))),
                                List.of(new OpsCopilotModelAction("Break glass", "/forbidden", "Do something unsupported.", List.of("unknown")))
                        )
                )
        );

        @SuppressWarnings("unchecked")
        ObjectProvider<OpsCopilotProvider> providerObjectProvider = mock(ObjectProvider.class);
        when(providerObjectProvider.getIfAvailable()).thenReturn(provider);

        OpsCopilotContextService contextService = mock(OpsCopilotContextService.class);
        when(contextService.buildContext(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OpsCopilotContext(
                        OpsCopilotScope.OPS_OVERVIEW,
                        "ops-overview",
                        null,
                        new ObjectMapper().createObjectNode(),
                        List.of(new OpsCopilotSource("alerts-current", "Alerts", "Current alerts")),
                        List.of("/ops/remediation")
                ));

        OpsCopilotPromptFactory promptFactory = mock(OpsCopilotPromptFactory.class);
        when(promptFactory.buildPrompt(org.mockito.ArgumentMatchers.any())).thenReturn("prompt");

        OpsCopilotService service = new OpsCopilotService(
                applicationProperties,
                providerObjectProvider,
                contextService,
                promptFactory,
                new SimpleMeterRegistry()
        );

        var response = service.analyze(new OpsCopilotAnalyzeRequest("OPS_OVERVIEW", null, null, null, null));

        assertThat(response.recommendedActions())
                .extracting(action -> action.href())
                .contains("/ops/remediation");
        assertThat(response.prioritizedFindings().get(0).sourceIds()).containsExactly("alerts-current");
        assertThat(response.citations()).extracting(citation -> citation.sourceId()).containsExactly("alerts-current");
    }

    private static final class StubProvider implements OpsCopilotProvider {

        private final OpsCopilotProviderResult result;

        private StubProvider(OpsCopilotProviderResult result) {
            this.result = result;
        }

        @Override
        public String providerName() {
            return "gemini";
        }

        @Override
        public String modelName() {
            return "gemini-test";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public OpsCopilotProviderResult analyze(String prompt, String requestId) {
            return result;
        }
    }
}
