package com.cmbchina.datadirect.caliber.config;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmProviderCapabilityRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmProviderCapabilityRegistryTest {

    @Test
    void shouldExposeOpenAiResponsesCapabilityForGpt54() {
        LlmProviderCapabilityRegistry.ProviderCapability capability =
                LlmProviderCapabilityRegistry.resolve("https://api.openai.com/v1/responses", "gpt-5.4");

        assertThat(capability.providerCode()).isEqualTo("OPENAI");
        assertThat(capability.providerLabel()).isEqualTo("OpenAI");
        assertThat(capability.supportsResponsesApi()).isTrue();
        assertThat(capability.supportsStructuredOutputs()).isTrue();
        assertThat(capability.supportsThinkingToggle()).isFalse();
    }

    @Test
    void shouldKeepCompatibleCapabilityForQwenChatCompletions() {
        LlmProviderCapabilityRegistry.ProviderCapability capability =
                LlmProviderCapabilityRegistry.resolve("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen3-max");

        assertThat(capability.providerCode()).isEqualTo("COMPATIBLE");
        assertThat(capability.providerLabel()).isEqualTo("兼容模式");
        assertThat(capability.supportsResponsesApi()).isFalse();
        assertThat(capability.supportsStructuredOutputs()).isFalse();
        assertThat(capability.supportsThinkingToggle()).isTrue();
    }
}
