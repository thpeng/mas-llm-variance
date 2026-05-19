package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextStatus;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreativeMarketingTextAnalysisIntegrationTest {

    @Test
    void analyzesCreativeMarketingTextDirectly() {
        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("0006-creative-marketing-text-run.json", runLog(List.of(
                        "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                                + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                                + "Für Reisende aus Deutschland ist Luzern ein ideales Ziel für Kultur, Natur und Erholung.",
                        "Luzern lädt mit See, Bergen und Altstadt zu einer besonderen Auszeit ein. "
                                + "Die Stadt verbindet Kultur und Natur auf engem Raum. "
                                + "Für deutsche Gäste ist Luzern schnell erreichbar und angenehm vielseitig.",
                        "Luzern begeistert mit seiner Lage am Vierwaldstättersee. "
                                + "Die Altstadt und die Kapellbrücke machen die Stadt zu einem idealen Reiseziel.",
                        "Die Stadt begeistert mit ihrer Lage am Vierwaldstättersee. "
                                + "Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. "
                                + "Für Reisende aus Deutschland ist sie ein ideales Ziel für Kultur, Natur und Erholung."
                ))),
                creativeConfig()
        );

        assertThat(result.route()).isNull();
        assertThat(result.factualTravelInfo()).isNull();
        assertThat(result.literalFormatTravelerGuidance()).isNull();
        assertThat(result.creativeMarketingText()).isNotNull();
        assertThat(result.creativeMarketingText().responseCount()).isEqualTo(4);
        assertThat(result.creativeMarketingText().successCount()).isEqualTo(2);
        assertThat(result.creativeMarketingText().outlierCount()).isEqualTo(2);
        assertThat(result.creativeMarketingText().successShare()).isEqualTo(0.5);
        assertThat(result.creativeMarketingText().outliers()).containsExactly(3, 4);
        assertThat(result.creativeMarketingText().sentenceCountMismatchCount()).isEqualTo(1);
        assertThat(result.creativeMarketingText().requiredTermMissingCount()).isEqualTo(1);
        assertThat(result.creativeMarketingText().extractions())
                .extracting("status")
                .containsExactly(
                        CreativeMarketingTextStatus.SUCCESS,
                        CreativeMarketingTextStatus.SUCCESS,
                        CreativeMarketingTextStatus.OUTLIER,
                        CreativeMarketingTextStatus.OUTLIER
                );
        assertThat(result.creativeMarketingText().syntactic().clusters()).hasSize(1);
        assertThat(result.literal().responseCount()).isEqualTo(4);
    }

    private static Analyzer analyzer() {
        return TestAnalyzerFactory.create(creativeConfig(), new FixedClock());
    }

    private static AnalysisConfig creativeConfig() {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                ClusteringAlgorithm.CREATIVE_MARKETING_TEXT,
                defaults.route(),
                defaults.factualTravelInfo(),
                defaults.literalFormatTravelerGuidance(),
                new CreativeMarketingTextConfig(3, "Luzern"),
                defaults.bleu(),
                defaults.rouge()
        );
    }

    private static RunLog runLog(List<String> responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-19T12:00:00+02:00");
        List<RunLogEntry> entries = java.util.stream.IntStream.range(0, responses.size())
                .mapToObj(index -> new RunLogEntry(index + 1, now, now, responses.get(index), null))
                .toList();
        return new RunLog(
                "0006-creative-marketing-text",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-4o",
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "Erstelle einen kurzen Werbetext.",
                entries
        );
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-19T12:30:00+02:00");
        }
    }
}
