package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import org.junit.jupiter.api.Test;

class AnswerChunkerTest {

    private final AnswerChunker chunker = new AnswerChunker(new TextTokenizer());

    @Test
    void splitsOnBlankLinesAndMergesSmallBlocks() {
        String answer = """
                Zuerich und Luzern.

                Interlaken und Bern.

                Genf und Basel.
                """;

        assertThat(chunker.chunk(answer, new ChunkConfig(5), 20))
                .containsExactly(
                        "Zuerich und Luzern.",
                        "Interlaken und Bern.",
                        "Genf und Basel."
                );
    }

    @Test
    void splitsLargeBlocksAtMaximumTokenCount() {
        String answer = "eins zwei drei vier fuenf sechs";

        assertThat(chunker.chunk(answer, new ChunkConfig(10), 3))
                .containsExactly("eins zwei drei", "vier fuenf sechs");
    }
}
