package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReasoningTest {

    @Test
    void mapsLmStudioValues() {
        assertThat(Reasoning.OFF.lmStudioValue()).isEqualTo("off");
        assertThat(Reasoning.LOW.lmStudioValue()).isEqualTo("low");
        assertThat(Reasoning.MEDIUM.lmStudioValue()).isEqualTo("medium");
        assertThat(Reasoning.HIGH.lmStudioValue()).isEqualTo("high");
        assertThatThrownBy(Reasoning.XHIGH::lmStudioValue)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LM Studio");
    }

    @Test
    void mapsGeminiValues() {
        assertThat(Reasoning.OFF.geminiThinkingLevel()).isEqualTo("minimal");
        assertThat(Reasoning.LOW.geminiThinkingLevel()).isEqualTo("low");
        assertThat(Reasoning.MEDIUM.geminiThinkingLevel()).isEqualTo("medium");
        assertThat(Reasoning.HIGH.geminiThinkingLevel()).isEqualTo("high");
        assertThatThrownBy(Reasoning.XHIGH::geminiThinkingLevel)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gemini");
    }

    @Test
    void mapsOpenAiValues() {
        assertThat(Reasoning.OFF.openAiReasoningEffort()).isEqualTo("none");
        assertThat(Reasoning.LOW.openAiReasoningEffort()).isEqualTo("low");
        assertThat(Reasoning.MEDIUM.openAiReasoningEffort()).isEqualTo("medium");
        assertThat(Reasoning.HIGH.openAiReasoningEffort()).isEqualTo("high");
        assertThat(Reasoning.XHIGH.openAiReasoningEffort()).isEqualTo("xhigh");
    }

    @Test
    void mapsAnthropicValues() {
        assertThatThrownBy(Reasoning.OFF::anthropicEffort)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disables Anthropic thinking");
        assertThat(Reasoning.LOW.anthropicEffort()).isEqualTo("low");
        assertThat(Reasoning.MEDIUM.anthropicEffort()).isEqualTo("medium");
        assertThat(Reasoning.HIGH.anthropicEffort()).isEqualTo("high");
        assertThatThrownBy(Reasoning.XHIGH::anthropicEffort)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anthropic");
    }

    @Test
    void rejectsOnAlias() {
        assertThatThrownBy(() -> Reasoning.parse("on"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown reasoning");
    }
}
