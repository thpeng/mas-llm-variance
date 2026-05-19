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
}
