package com.codex.flashsale.api;

import java.util.List;

public record OpsCopilotAnalyzeResponse(
        String scope,
        String subjectId,
        String summary,
        List<OpsCopilotFindingResponse> prioritizedFindings,
        List<OpsCopilotActionResponse> recommendedActions,
        List<OpsCopilotCitationResponse> citations,
        OpsCopilotProviderMetadataResponse providerMetadata
) {
}
