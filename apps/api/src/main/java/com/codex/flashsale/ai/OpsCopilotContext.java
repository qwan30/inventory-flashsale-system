package com.codex.flashsale.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record OpsCopilotContext(
        OpsCopilotScope scope,
        String subjectId,
        String focusQuestion,
        JsonNode facts,
        List<OpsCopilotSource> sources,
        List<String> allowedHrefs
) {
}
