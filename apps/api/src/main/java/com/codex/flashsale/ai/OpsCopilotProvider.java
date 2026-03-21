package com.codex.flashsale.ai;

public interface OpsCopilotProvider {

    String providerName();

    String modelName();

    boolean isConfigured();

    OpsCopilotProviderResult analyze(String prompt, String requestId);
}
