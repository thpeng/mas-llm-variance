package ch.thp.mas.llm.variance.analyze.literal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LiteralAnalyzerTest {

    private final LiteralAnalyzer analyzer = new LiteralAnalyzer();

    @Test
    void reportsCompleteLiteralStabilityForIdenticalResponses() {
        LiteralAnalysis result = analyzer.analyze(List.of("Bern", "Bern", "Bern"));

        assertThat(result.allResponsesIdentical()).isTrue();
        assertThat(result.responseCount()).isEqualTo(3);
        assertThat(result.distinctResponseCount()).isEqualTo(1);
        assertThat(result.exactMatchRate()).isEqualTo(1.0);
    }

    @Test
    void treatsPunctuationCasingAndWhitespaceAsLiteralDifferences() {
        LiteralAnalysis result = analyzer.analyze(List.of("Bern", "bern", "Bern.", "Bern\n"));

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.responseCount()).isEqualTo(4);
        assertThat(result.distinctResponseCount()).isEqualTo(4);
        assertThat(result.exactMatchRate()).isEqualTo(0.0);
    }

    @Test
    void reportsPartialExactMatchesAcrossAllPairs() {
        LiteralAnalysis result = analyzer.analyze(List.of("A", "A", "B"));

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.responseCount()).isEqualTo(3);
        assertThat(result.distinctResponseCount()).isEqualTo(2);
        assertThat(result.exactMatchRate()).isEqualTo(1.0 / 3.0);
    }

    @Test
    void treatsSingleResponseAsStableBecauseThereIsNoPairwiseContradiction() {
        LiteralAnalysis result = analyzer.analyze(List.of("A"));

        assertThat(result.allResponsesIdentical()).isTrue();
        assertThat(result.responseCount()).isEqualTo(1);
        assertThat(result.distinctResponseCount()).isEqualTo(1);
        assertThat(result.exactMatchRate()).isEqualTo(1.0);
    }

    @Test
    void reportsEmptyInputAsNoLiteralStability() {
        LiteralAnalysis result = analyzer.analyze(List.of());

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.responseCount()).isZero();
        assertThat(result.distinctResponseCount()).isZero();
        assertThat(result.exactMatchRate()).isEqualTo(0.0);
    }
}
