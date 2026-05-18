package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InferenceProviderTest {

    @Test
    void geminiHasDefaultModel() {
        assertThat(InferenceProvider.GEMINI.defaultModel()).isEqualTo("gemini-3-flash");
    }
}
