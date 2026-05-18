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
        assertThat(Reasoning.OFF.anthropicThinkingLevel()).isEqualTo("low");
        assertThat(Reasoning.LOW.anthropicThinkingLevel()).isEqualTo("medium");
        assertThat(Reasoning.MEDIUM.anthropicThinkingLevel()).isEqualTo("high");
        assertThat(Reasoning.HIGH.anthropicThinkingLevel()).isEqualTo("xhigh");
        assertThat(Reasoning.XHIGH.anthropicThinkingLevel()).isEqualTo("max");
    }

    @Test
    void rejectsOnAlias() {
        assertThatThrownBy(() -> Reasoning.parse("on"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown reasoning");
    }
}
