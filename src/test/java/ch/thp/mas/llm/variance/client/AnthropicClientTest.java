package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnthropicClientTest {

    @Test
    void usesTemperatureWhenItIsNonZero() {
        LlmRequestConfig config = new LlmRequestConfig("claude", 0.2, 1.0, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isTrue();
        assertThat(AnthropicClient.useTopP(config)).isFalse();
    }

    @Test
    void usesTopPWhenTemperatureIsZero() {
        LlmRequestConfig config = new LlmRequestConfig("claude", 0.0, 1.0, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isFalse();
        assertThat(AnthropicClient.useTopP(config)).isTrue();
    }

    @Test
    void usesTopPWhenTemperatureIsUnset() {
        LlmRequestConfig config = new LlmRequestConfig("claude", null, 0.9, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isFalse();
        assertThat(AnthropicClient.useTopP(config)).isTrue();
    }

    @Test
    void omitsBothWhenTemperatureIsZeroAndTopPIsUnset() {
        LlmRequestConfig config = new LlmRequestConfig("claude", 0.0, null, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isFalse();
        assertThat(AnthropicClient.useTopP(config)).isFalse();
    }
}
