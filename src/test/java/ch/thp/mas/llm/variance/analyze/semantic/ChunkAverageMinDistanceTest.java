package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.EmbeddingResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkAverageMinDistanceTest {

    private final ChunkAverageMinDistance distance = new ChunkAverageMinDistance(new CosineDistance());

    @Test
    void returnsZeroWhenEachChunkHasAMatchingChunk() {
        List<EmbeddingResult> left = List.of(embedding(1, 0), embedding(0, 1));
        List<EmbeddingResult> right = List.of(embedding(0, 1), embedding(1, 0));

        assertThat(distance.distance(left, right)).isEqualTo(0.0);
    }

    @Test
    void penalizesExtraUnmatchedContentSymmetrically() {
        List<EmbeddingResult> left = List.of(embedding(1, 0));
        List<EmbeddingResult> right = List.of(embedding(1, 0), embedding(0, 1));

        assertThat(distance.distance(left, right)).isEqualTo(0.25);
    }

    private static EmbeddingResult embedding(double... values) {
        return new EmbeddingResult(values, false);
    }
}
