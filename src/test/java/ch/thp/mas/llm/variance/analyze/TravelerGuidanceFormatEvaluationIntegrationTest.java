package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatClassification;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatConfig;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerGuidanceFormatEvaluationIntegrationTest {

    private static final String REFERENCE =
            "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.";

    @Test
    void analyzestravelerGuidanceFormatDirectly() {
        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("0005-literal-format-traveler-guidance-run.json", runLog(List.of(
                        REFERENCE,
                        "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf\n"
                                + "die Linie S3.",
                        REFERENCE + " Bitte beachten Sie die Anzeigen am Bahnhof.",
                        "Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5 oder S12.",
                        "Reisende von Bern nach Zürich sollen von Bern bis Bern Wankdorf die Linie S3 verwenden."
                ))),
                literalFormatConfig()
        );

        assertThat(result.swissRoundTrip()).isNull();
        assertThat(result.bernZurichConnection()).isNull();
        assertThat(result.travelerGuidanceFormat()).isNotNull();
        assertThat(result.travelerGuidanceFormat().responseCount()).isEqualTo(5);
        assertThat(result.travelerGuidanceFormat().exactMatchCount()).isEqualTo(1);
        assertThat(result.travelerGuidanceFormat().normalizedExactMatchCount()).isEqualTo(1);
        assertThat(result.travelerGuidanceFormat().noMatchCount()).isEqualTo(3);
        assertThat(result.travelerGuidanceFormat().outliers()).containsExactly(3, 4, 5);
        assertThat(result.travelerGuidanceFormat().forbiddenTemplateTermCounts())
                .containsEntry("Sargans", 1)
                .containsEntry("Chur", 1)
                .containsEntry("Landquart", 1)
                .containsEntry("RE5", 1)
                .containsEntry("S12", 1);
        assertThat(result.travelerGuidanceFormat().additionalSentenceCandidateCount()).isEqualTo(1);
        assertThat(result.travelerGuidanceFormat().extractions())
                .extracting("classification")
                .containsExactly(
                        TravelerGuidanceFormatClassification.EXACT_MATCH,
                        TravelerGuidanceFormatClassification.NORMALIZED_EXACT_MATCH,
                        TravelerGuidanceFormatClassification.NO_MATCH,
                        TravelerGuidanceFormatClassification.NO_MATCH,
                        TravelerGuidanceFormatClassification.NO_MATCH
                );
        assertThat(result.literal().responseCount()).isEqualTo(5);
    }

    private static Analyzer analyzer() {
        return TestAnalyzerFactory.create(literalFormatConfig(), new FixedClock());
    }

    private static AnalysisConfig literalFormatConfig() {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                PromptEvaluation.LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE,
                defaults.swissRoundTrip(),
                defaults.bernZurichConnection(),
                new TravelerGuidanceFormatConfig(REFERENCE),
                defaults.lucerneMarketingText(),
                defaults.bleu(),
                defaults.rouge()
        );
    }

    private static RunLog runLog(List<String> responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-19T11:00:00+02:00");
        List<RunLogEntry> entries = java.util.stream.IntStream.range(0, responses.size())
                .mapToObj(index -> new RunLogEntry(index + 1, now, now, responses.get(index), null))
                .toList();
        return new RunLog(
                "0005-literal-format-traveler-guidance",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-4o",
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "Formuliere den Reisehinweis.",
                entries
        );
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-19T11:30:00+02:00");
        }
    }
}
