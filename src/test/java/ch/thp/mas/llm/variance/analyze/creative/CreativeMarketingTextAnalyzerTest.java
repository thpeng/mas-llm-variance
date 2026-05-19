package ch.thp.mas.llm.variance.analyze.creative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CreativeMarketingTextAnalyzerTest {

    private final CreativeMarketingTextAnalyzer analyzer = new CreativeMarketingTextAnalyzer();
    private final CreativeMarketingTextConfig config = new CreativeMarketingTextConfig(3, "Luzern");

    @Test
    void classifiesThreeSentencesWithLuzernAsSuccess() {
        CreativeMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                        + "Für Reisende aus Deutschland ist Luzern ein ideales Ziel für Kultur, Natur und Erholung.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(3);
        assertThat(extraction.containsRequiredTerm()).isTrue();
        assertThat(extraction.status()).isEqualTo(CreativeMarketingTextStatus.SUCCESS);
        assertThat(extraction.failureReasons()).isEmpty();
    }

    @Test
    void reportsSentenceCountMismatch() {
        CreativeMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke machen die Stadt zu einem idealen Reiseziel.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(2);
        assertThat(extraction.status()).isEqualTo(CreativeMarketingTextStatus.OUTLIER);
        assertThat(extraction.failureReasons()).containsExactly("sentence_count_mismatch");
    }

    @Test
    void reportsMissingRequiredTermCaseSensitively() {
        CreativeMarketingTextExtraction extraction = analyzer.extract(
                1,
                "Die Stadt begeistert mit ihrer Lage am Vierwaldstättersee. "
                        + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                        + "Für Reisende aus Deutschland ist lucern ein ideales Ziel für Kultur, Natur und Erholung.",
                config
        );

        assertThat(extraction.sentenceCount()).isEqualTo(3);
        assertThat(extraction.containsRequiredTerm()).isFalse();
        assertThat(extraction.status()).isEqualTo(CreativeMarketingTextStatus.OUTLIER);
        assertThat(extraction.failureReasons()).containsExactly("required_term_missing");
    }

    @Test
    void countsRepeatedSentenceEndPunctuationAsOneSentenceEnd() {
        assertThat(analyzer.sentenceCount("Luzern begeistert! Wirklich? Sehr sogar!!")).isEqualTo(3);
    }

    @Test
    void aggregatesAnalysis() {
        CreativeMarketingTextAnalysis analysis = analyzer.analyze(
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
