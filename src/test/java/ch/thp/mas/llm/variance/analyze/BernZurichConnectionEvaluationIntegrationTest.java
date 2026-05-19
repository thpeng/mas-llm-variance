package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionStatus;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BernZurichConnectionEvaluationIntegrationTest {

    @Test
    void analyzesCriticalTravelFactsDirectly() {
        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("0004-critical-travel-info-run.json", runLog(List.of(
                        "Die Verbindung faehrt um 08:02 ab Bern, kommt um 09:15 in Zuerich HB an und hat keine Umstiege.",
                        "Abfahrt ab Bern: 08:02, Ankunft Zuerich HB: 09:15, Umstiege: keine.",
                        "Abfahrt 08.02, Ankunft 9.15, 0 Umstiege.",
                        "Die Verbindung faehrt um 06:45 ab Lausanne und kommt um 09:15 in Zuerich HB an.",
                        "Abfahrt 08:34, Ankunft 09:15, keine Umstiege."
                ))),
                factualConfig()
        );

        assertThat(result.swissRoundTrip()).isNull();
        assertThat(result.bernZurichConnection()).isNotNull();
        assertThat(result.bernZurichConnection().responseCount()).isEqualTo(5);
        assertThat(result.bernZurichConnection().successCount()).isEqualTo(3);
        assertThat(result.bernZurichConnection().outlierCount()).isEqualTo(2);
        assertThat(result.bernZurichConnection().successShare()).isEqualTo(0.6);
        assertThat(result.bernZurichConnection().outliers()).containsExactly(4, 5);
        assertThat(result.bernZurichConnection().departureFoundCount()).isEqualTo(3);
        assertThat(result.bernZurichConnection().arrivalFoundCount()).isEqualTo(5);
        assertThat(result.bernZurichConnection().changesFoundCount()).isEqualTo(4);
        assertThat(result.bernZurichConnection().extraTimeCounts())
                .containsEntry("06:45", 1)
                .containsEntry("08:34", 1);
        assertThat(result.bernZurichConnection().extractions())
                .extracting("status")
                .containsExactly(
                        BernZurichConnectionStatus.SUCCESS,
                        BernZurichConnectionStatus.SUCCESS,
                        BernZurichConnectionStatus.SUCCESS,
                        BernZurichConnectionStatus.OUTLIER,
                        BernZurichConnectionStatus.OUTLIER
                );
        assertThat(result.bernZurichConnection().extractions().get(3).failureReasons())
                .containsExactly("departure_missing", "changes_missing");
        assertThat(result.bernZurichConnection().extractions().get(4).failureReasons())
                .containsExactly("departure_missing");
        assertThat(result.bernZurichConnection().syntactic().clusters()).hasSize(1);
        assertThat(result.literal().responseCount()).isEqualTo(5);
    }

    private static Analyzer analyzer() {
        return TestAnalyzerFactory.create(factualConfig(), new FixedClock());
    }

    private static AnalysisConfig factualConfig() {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                PromptEvaluation.FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION,
                defaults.swissRoundTrip(),
                new BernZurichConnectionConfig("08:02", "09:15", 0),
                defaults.travelerGuidanceFormat(),
                defaults.lucerneMarketingText(),
                defaults.bleu(),
                defaults.rouge()
        );
    }

    private static RunLog runLog(List<String> responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-19T10:00:00+02:00");
        List<RunLogEntry> entries = java.util.stream.IntStream.range(0, responses.size())
                .mapToObj(index -> new RunLogEntry(index + 1, now, now, responses.get(index), null))
                .toList();
        return new RunLog(
                "0004-critical-travel-info",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-4o",
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "Extrahiere Reiseinformationen.",
                entries
        );
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-19T10:30:00+02:00");
        }
    }
}
