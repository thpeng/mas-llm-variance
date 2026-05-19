package ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LucerneMarketingTextEvaluatorTest {

    private final LucerneMarketingTextEvaluator analyzer = new LucerneMarketingTextEvaluator();
    private final LucerneMarketingTextConfig config = new LucerneMarketingTextConfig(3, "Luzern");

    @Test
    void classifiesThreeSentencesWithLuzernAsSuccess() {
        LucerneMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                        + "Für Reisende aus Deutschland ist Luzern ein ideales Ziel für Kultur, Natur und Erholung.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(3);
        assertThat(extraction.containsRequiredTerm()).isTrue();
        assertThat(extraction.status()).isEqualTo(LucerneMarketingTextStatus.SUCCESS);
        assertThat(extraction.failureReasons()).isEmpty();
    }

    @Test
    void reportsSentenceCountMismatch() {
        LucerneMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke machen die Stadt zu einem idealen Reiseziel.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(2);
        assertThat(extraction.status()).isEqualTo(LucerneMarketingTextStatus.OUTLIER);
        assertThat(extraction.failureReasons()).containsExactly("sentence_count_mismatch");
    }

    @Test
    void reportsMissingRequiredTermCaseSensitively() {
        LucerneMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Die Stadt begeistert mit ihrer Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                        + "Für Reisende aus Deutschland ist lucern ein ideales Ziel für Kultur, Natur und Erholung.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(3);
        assertThat(extraction.containsRequiredTerm()).isFalse();
        assertThat(extraction.status()).isEqualTo(LucerneMarketingTextStatus.OUTLIER);
        assertThat(extraction.failureReasons()).containsExactly("required_term_missing");
    }

    @Test
    void countsRepeatedSentenceEndPunctuationAsOneSentenceEnd() {
        assertThat(analyzer.sentenceCount("Luzern begeistert! Wirklich? Sehr sogar!!")).isEqualTo(3);
    }

    @Test
    void aggregatesAnalysis() {
        LucerneMarketingTextEvaluation analysis = analyzer.analyze(
                List.of(
                        "Luzern begeistert am See. Die Altstadt ist charmant. Die Berge liegen nah.",
                        "Luzern begeistert am See. Die Altstadt ist charmant.",
                        "Die Stadt begeistert am See. Die Altstadt ist charmant. Die Berge liegen nah."
                ),
                config,
                successIndices -> null
        );

        assertThat(analysis.responseCount()).isEqualTo(3);
        assertThat(analysis.successCount()).isEqualTo(1);
        assertThat(analysis.outlierCount()).isEqualTo(2);
        assertThat(analysis.successShare()).isEqualTo(1.0 / 3.0);
        assertThat(analysis.outliers()).containsExactly(2, 3);
        assertThat(analysis.sentenceCountMismatchCount()).isEqualTo(1);
        assertThat(analysis.requiredTermMissingCount()).isEqualTo(1);
    }
}
