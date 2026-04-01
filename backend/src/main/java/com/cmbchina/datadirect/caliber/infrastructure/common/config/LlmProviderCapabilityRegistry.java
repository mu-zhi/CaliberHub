package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import java.net.URI;
import java.util.Locale;

public final class LlmProviderCapabilityRegistry {

    private LlmProviderCapabilityRegistry() {
    }

    public static ProviderCapability resolve(String endpoint, String model) {
        String normalizedEndpoint = safe(endpoint).toLowerCase(Locale.ROOT);
        String normalizedModel = safe(model).toLowerCase(Locale.ROOT);
        String host = extractHost(normalizedEndpoint);
        boolean openAiEndpoint = host.contains("api.openai.com") || normalizedEndpoint.contains("/v1/responses");

        if (openAiEndpoint) {
            boolean supportsResponsesApi = normalizedEndpoint.contains("/v1/responses");
            boolean supportsStructuredOutputs = normalizedModel.startsWith("gpt-5") || normalizedModel.startsWith("gpt-4.1");
            return new ProviderCapability(
                    "OPENAI",
                    "OpenAI",
                    supportsResponsesApi,
                    supportsStructuredOutputs,
                    false
            );
        }

        return new ProviderCapability(
                "COMPATIBLE",
                "兼容模式",
                false,
                false,
                true
        );
    }

    private static String extractHost(String endpoint) {
        try {
            return safe(URI.create(endpoint).getHost()).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record ProviderCapability(
            String providerCode,
            String providerLabel,
            boolean supportsResponsesApi,
            boolean supportsStructuredOutputs,
            boolean supportsThinkingToggle
    ) {
    }
}
