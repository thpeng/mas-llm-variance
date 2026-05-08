package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerChunkerTest {

    private final AnswerChunker chunker = new AnswerChunker(new TextTokenizer());

    @Test
    void splitsOnBlankLinesAndMergesSmallBlocks() {
        String answer = """
                Zürich und Luzern.

                Interlaken und Bern.

                Genf und Basel.
                """;

        assertThat(chunker.chunk(answer, new ChunkConfig(5), 20))
                .containsExactly(
                        "Zürich und Luzern.",
                        "Interlaken und Bern.",
                        "Genf und Basel."
                );
    }

    @Test
    void splitsLargeBlocksAtMaximumTokenCount() {
        String answer = "eins zwei drei vier fünf sechs";

        assertThat(chunker.chunk(answer, new ChunkConfig(10), 3))
                .containsExactly("eins zwei drei", "vier fünf sechs");
    }
}
