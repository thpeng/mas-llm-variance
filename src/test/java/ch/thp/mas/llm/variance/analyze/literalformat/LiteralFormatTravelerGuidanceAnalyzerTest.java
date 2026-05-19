package ch.thp.mas.llm.variance.analyze.literalformat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LiteralFormatTravelerGuidanceAnalyzerTest {

    private static final String REFERENCE =
            "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.";

    private final LiteralFormatTravelerGuidanceAnalyzer analyzer = new LiteralFormatTravelerGuidanceAnalyzer();
    private final LiteralFormatTravelerGuidanceConfig config = new LiteralFormatTravelerGuidanceConfig(REFERENCE);

    @Test
    void classifiesExactMatch() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(1, REFERENCE, config);

        assertThat(extraction.exactMatch()).isTrue();
        assertThat(extraction.normalizedExactMatch()).isTrue();
        assertThat(extraction.classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.EXACT_MATCH);
    }

    @Test
    void classifiesWhitespaceOnlyDifferenceAsNormalizedExactMatch() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(
                1,
                "  Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf\n  die Linie S3.  ",
                config
        );

        assertThat(extraction.exactMatch()).isFalse();
        assertThat(extraction.normalizedExactMatch()).isTrue();
        assertThat(extraction.classification())
                .isEqualTo(LiteralFormatTravelerGuidanceClassification.NORMALIZED_EXACT_MATCH);
    }

    @Test
    void doesNotNormalizeCaseOrPunctuation() {
        assertThat(analyzer.extract(
                1,
                "reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.",
                config
        ).classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.NO_MATCH);

        assertThat(analyzer.extract(
                2,
                "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3",
                config
        ).classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.NO_MATCH);
    }

    @Test
    void detectsForbiddenTemplateContentWithTokenBoundaries() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(
                1,
                "Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5 oder S12.",
                config
        );

        assertThat(extraction.classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.NO_MATCH);
        assertThat(extraction.containsForbiddenTemplateContent()).isTrue();
        assertThat(extraction.forbiddenTemplateTerms())
                .containsExactly("Sargans", "Chur", "Landquart", "RE5", "S12");
    }

    @Test
    void doesNotMatchS1InsideLongerLineIdentifiers() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(
                1,
                "Reisende ab Bern bis Zürich benützen die Linie S12 oder S10.",
                config
        );

        assertThat(extraction.forbiddenTemplateTerms()).containsExactly("S12");
    }

    @Test
    void detectsAdditionalSentenceCandidate() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(
                1,
                REFERENCE + " Bitte beachten Sie die Anzeigen am Bahnhof.",
                config
        );

        assertThat(extraction.classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.NO_MATCH);
        assertThat(extraction.containsAdditionalSentenceCandidate()).isTrue();
    }

    @Test
    void paraphraseIsNoMatchButDiagnosticFlagsAreSet() {
        LiteralFormatTravelerGuidanceExtraction extraction = analyzer.extract(
                1,
                "Reisende von Bern nach Zürich sollen von Bern bis Bern Wankdorf die Linie S3 verwenden.",
                config
        );

        assertThat(extraction.classification()).isEqualTo(LiteralFormatTravelerGuidanceClassification.NO_MATCH);
        assertThat(extraction.hasBern()).isTrue();
        assertThat(extraction.hasZurich()).isTrue();
        assertThat(extraction.hasBernWankdorf()).isTrue();
        assertThat(extraction.hasS3()).isTrue();
    }

    @Test
    void aggregatesAnalysis() {
        LiteralFormatTravelerGuidanceAnalysis analysis = analyzer.analyze(
                List.of(
                        REFERENCE,
                        "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf\n"
                                + "die Linie S3.",
                        "Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5 oder S12."
                ),
                config
        );

        assertThat(analysis.responseCount()).isEqualTo(3);
        assertThat(analysis.exactMatchCount()).isEqualTo(1);
        assertThat(analysis.normalizedExactMatchCount()).isEqualTo(1);
        assertThat(analysis.noMatchCount()).isEqualTo(1);
        assertThat(analysis.exactMatchShare()).isEqualTo(1.0 / 3.0);
        assertThat(analysis.normalizedAcceptedShare()).isEqualTo(2.0 / 3.0);
        assertThat(analysis.outliers()).containsExactly(3);
        assertThat(analysis.forbiddenTemplateTermCounts()).containsEntry("Sargans", 1);
    }
}
