package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenAiClientTest {

    @Test
    void detectsOpenAiModelsWithReasoningSupport() {
        assertThat(OpenAiClient.supportsReasoning("o1-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("o3-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("o4-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("gpt-5-mini-2025-08-07")).isTrue();
    }

    @Test
    void detectsOpenAiModelsWithoutReasoningSupport() {
        assertThat(OpenAiClient.supportsReasoning("gpt-4o")).isFalse();
        assertThat(OpenAiClient.supportsReasoning("gpt-4o-mini")).isFalse();
        assertThat(OpenAiClient.supportsReasoning("gpt-4.1")).isFalse();
        assertThat(OpenAiClient.supportsReasoning(null)).isFalse();
        assertThat(OpenAiClient.supportsReasoning("")).isFalse();
    }

    @Test
    void omitsSamplingParametersForReasoningModelWhenReasoningIsEnabled() {
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.LOW))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.MEDIUM))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.HIGH))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.XHIGH))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.OFF))).isTrue();
        assertThat(OpenAiClient.sendsSamplingParameters(
                new LlmRequestConfig("gpt-5.4-mini-2026-03-17", 0.0, 1.0, 1, 1L, Reasoning.HIGH, false)))
                .isTrue();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-4o-2024-11-20", Reasoning.LOW))).isTrue();
    }

    private static LlmRequestConfig config(String model, Reasoning reasoning) {
        return new LlmRequestConfig(model, 0.0, 1.0, 1, 1L, reasoning);
    }
}
