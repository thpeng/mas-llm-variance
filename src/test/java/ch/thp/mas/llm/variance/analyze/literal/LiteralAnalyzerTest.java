package ch.thp.mas.llm.variance.analyze.literal;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.semantic.SemanticCluster;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiteralAnalyzerTest {

    private final LiteralAnalyzer analyzer = new LiteralAnalyzer();

    @Test
    void reportsCompleteLiteralStabilityForIdenticalResponses() {
        LiteralAnalysis result = analyzer.analyze(
                List.of("Bern", "Bern", "Bern"),
                List.of(cluster(0, 1, 2, 3))
        );

        assertThat(result.allResponsesIdentical()).isTrue();
        assertThat(result.responseCount()).isEqualTo(3);
        assertThat(result.distinctResponseCount()).isEqualTo(1);
        assertThat(result.exactMatchRate()).isEqualTo(1.0);
        assertThat(result.clusters()).hasSize(1);
        assertThat(result.clusters().getFirst().pairCount()).isEqualTo(3);
        assertThat(result.clusters().getFirst().exactMatchPairCount()).isEqualTo(3);
        assertThat(result.clusters().getFirst().exactMatchRate()).isEqualTo(1.0);
    }

    @Test
    void treatsPunctuationCasingAndWhitespaceAsLiteralDifferences() {
        LiteralAnalysis result = analyzer.analyze(
                List.of("Bern", "bern", "Bern.", "Bern\n"),
                List.of(cluster(0, 1, 2, 3, 4))
        );

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.distinctResponseCount()).isEqualTo(4);
        assertThat(result.exactMatchRate()).isEqualTo(0.0);
        assertThat(result.clusters().getFirst().exactMatchRate()).isEqualTo(0.0);
    }

    @Test
    void reportsPartialExactMatchesAcrossAllPairs() {
        LiteralAnalysis result = analyzer.analyze(
                List.of("A", "A", "B"),
                List.of(cluster(0, 1, 2, 3))
        );

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.distinctResponseCount()).isEqualTo(2);
        assertThat(result.exactMatchRate()).isEqualTo(1.0 / 3.0);
        assertThat(result.clusters().getFirst().pairCount()).isEqualTo(3);
        assertThat(result.clusters().getFirst().exactMatchPairCount()).isEqualTo(1);
    }

    @Test
    void treatsSingleResponseClusterAsStableBecauseThereIsNoPairwiseContradiction() {
        LiteralAnalysis result = analyzer.analyze(
                List.of("A", "B"),
                List.of(cluster(0, 1), cluster(1, 2))
        );

        assertThat(result.allResponsesIdentical()).isFalse();
        assertThat(result.exactMatchRate()).isEqualTo(0.0);
        assertThat(result.clusters()).extracting(LiteralClusterAnalysis::pairCount)
                .containsExactly(0, 0);
        assertThat(result.clusters()).extracting(LiteralClusterAnalysis::exactMatchRate)
                .containsExactly(1.0, 1.0);
    }

    @Test
    void includesOutliersInRunLevelMetricsButNotClusterMetrics() {
        LiteralAnalysis result = analyzer.analyze(
                List.of("A", "A", "B"),
                List.of(cluster(0, 1, 2))
        );

        assertThat(result.responseCount()).isEqualTo(3);
        assertThat(result.distinctResponseCount()).isEqualTo(2);
        assertThat(result.exactMatchRate()).isEqualTo(1.0 / 3.0);
        assertThat(result.clusters()).hasSize(1);
        assertThat(result.clusters().getFirst().responseCount()).isEqualTo(2);
        assertThat(result.clusters().getFirst().distinctResponseCount()).isEqualTo(1);
        assertThat(result.clusters().getFirst().exactMatchRate()).isEqualTo(1.0);
    }

    private static SemanticCluster cluster(int clusterId, Integer... repetitionIndices) {
        return new SemanticCluster(
                clusterId,
                repetitionIndices.length,
                List.of(repetitionIndices),
                repetitionIndices[0],
                null
        );
    }
}
