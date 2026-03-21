package com.codex.flashsale.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpsCopilotPromptFactory {

    private final ObjectMapper objectMapper;

    public OpsCopilotPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPrompt(OpsCopilotContext context) {
        return """
                You are an advisory-only operations copilot for an inventory and flash-sale system.

                Rules:
                - Use ONLY the provided operational facts.
                - Do not invent hidden state, root causes, or actions outside the provided evidence.
                - Do not suggest direct database edits, shell access, or code changes as operator actions.
                - Recommended actions MUST use one of the allowed href values exactly.
                - Findings and actions MUST cite only the listed sourceIds.
                - Return JSON only. No markdown fences. No prose outside JSON.

                Output JSON schema:
                {
                  "summary": "string",
                  "prioritizedFindings": [
                    {
                      "severity": "INFO|WARN|CRITICAL",
                      "title": "string",
                      "detail": "string",
                      "sourceIds": ["string"]
                    }
                  ],
                  "recommendedActions": [
                    {
                      "label": "string",
                      "href": "string",
                      "rationale": "string",
                      "sourceIds": ["string"]
                    }
                  ]
                }

                Scope: %s
                Subject: %s
                Focus question: %s
                Allowed hrefs: %s
                Available sources: %s

                Operational facts:
                %s
                """.formatted(
                context.scope().name(),
                context.subjectId(),
                normalizeQuestion(context.focusQuestion()),
                toJson(context.allowedHrefs()),
                toJson(context.sources()),
                toJson(context.facts())
        );
    }

    private String normalizeQuestion(String focusQuestion) {
        return focusQuestion == null || focusQuestion.isBlank()
                ? "No additional question provided. Summarize the highest-priority risks and next operator moves."
                : focusQuestion;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize ops copilot prompt context", exception);
        }
    }
}
