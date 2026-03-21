package com.codex.flashsale.ai;

import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@ConditionalOnExpression("'${app.ai.provider:gemini}' == 'gemini'")
public class GeminiOpsCopilotProvider implements OpsCopilotProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties.Ai aiProperties;
    private final ApplicationProperties.Gemini geminiProperties;

    public GeminiOpsCopilotProvider(
            ApplicationProperties applicationProperties,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.aiProperties = applicationProperties.getAi();
        this.geminiProperties = applicationProperties.getAi().getGemini();
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory(
                        aiProperties.getConnectTimeout(),
                        aiProperties.getReadTimeout()
                ))
                .build();
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public String modelName() {
        return geminiProperties.getModel();
    }

    @Override
    public boolean isConfigured() {
        return nonBlank(geminiProperties.getApiKey())
                && nonBlank(geminiProperties.getBaseUrl())
                && nonBlank(geminiProperties.getModel());
    }

    @Override
    public OpsCopilotProviderResult analyze(String prompt, String requestId) {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini ops copilot is not fully configured");
        }

        RuntimeException lastFailure = null;
        int maxAttempts = Math.max(0, aiProperties.getRetryCount()) + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .scheme(baseUri().getScheme())
                                .host(baseUri().getHost())
                                .port(baseUri().getPort())
                                .path(path())
                                .queryParam("key", geminiProperties.getApiKey())
                                .build(geminiProperties.getModel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody(prompt))
                        .retrieve()
                        .body(JsonNode.class);
                return new OpsCopilotProviderResult(requestId, parseResponse(response));
            } catch (ResourceAccessException exception) {
                lastFailure = new IllegalStateException("Gemini connection failed: " + exception.getMessage(), exception);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is4xxClientError()) {
                    throw new IllegalStateException("Gemini rejected the request: " + exception.getMessage(), exception);
                }
                lastFailure = new IllegalStateException("Gemini request failed: " + exception.getMessage(), exception);
            } catch (RestClientException exception) {
                lastFailure = new IllegalStateException("Gemini request failed: " + exception.getMessage(), exception);
            }
        }

        throw lastFailure == null
                ? new IllegalStateException("Gemini request failed unexpectedly")
                : lastFailure;
    }

    private ObjectNode requestBody(String prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", aiProperties.getMaxOutputTokens());
        return body;
    }

    private OpsCopilotModelResponse parseResponse(JsonNode response) {
        String candidateText = response.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText(null);

        if (!nonBlank(candidateText)) {
            throw new IllegalStateException("Gemini returned no content for ops copilot");
        }
        if (candidateText.length() > aiProperties.getMaxResponseChars()) {
            throw new IllegalStateException("Gemini response exceeded the configured ops copilot response budget");
        }

        String jsonText = extractJson(candidateText);
        try {
            return objectMapper.readValue(jsonText, OpsCopilotModelResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gemini returned invalid ops copilot JSON", exception);
        }
    }

    private String extractJson(String candidateText) {
        String trimmed = candidateText.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        throw new IllegalStateException("Gemini response did not contain a JSON object");
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }

    private String path() {
        String normalizedPath = baseUri().getPath();
        if (normalizedPath == null || normalizedPath.isBlank() || "/".equals(normalizedPath)) {
            return "/v1beta/models/{model}:generateContent";
        }
        return normalizedPath + "/v1beta/models/{model}:generateContent";
    }

    private URI baseUri() {
        return URI.create(geminiProperties.getBaseUrl());
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
