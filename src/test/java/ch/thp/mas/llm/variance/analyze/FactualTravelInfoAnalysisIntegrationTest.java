package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalyzer;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoConfig;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoStatus;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor;
import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FactualTravelInfoAnalysisIntegrationTest {

    @Test
    void analyzesCriticalTravelFactsWithoutEmbeddingService() {
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

        assertThat(result.scans()).isEmpty();
        assertThat(result.route()).isNull();
        assertThat(result.factualTravelInfo()).isNotNull();
        assertThat(result.factualTravelInfo().responseCount()).isEqualTo(5);
        assertThat(result.factualTravelInfo().successCount()).isEqualTo(3);
        assertThat(result.factualTravelInfo().outlierCount()).isEqualTo(2);
        assertThat(result.factualTravelInfo().successShare()).isEqualTo(0.6);
        assertThat(result.factualTravelInfo().outliers()).containsExactly(4, 5);
        assertThat(result.factualTravelInfo().departureFoundCount()).isEqualTo(3);
        assertThat(result.factualTravelInfo().arrivalFoundCount()).isEqualTo(5);
        assertThat(result.factualTravelInfo().changesFoundCount()).isEqualTo(4);
        assertThat(result.factualTravelInfo().extraTimeCounts())
                .containsEntry("06:45", 1)
                .containsEntry("08:34", 1);
        assertThat(result.factualTravelInfo().extractions())
                .extracting("status")
                .containsExactly(
                        FactualTravelInfoStatus.SUCCESS,
                        FactualTravelInfoStatus.SUCCESS,
                        FactualTravelInfoStatus.SUCCESS,
                        FactualTravelInfoStatus.OUTLIER,
                        FactualTravelInfoStatus.OUTLIER
                );
        assertThat(result.factualTravelInfo().extractions().get(3).failureReasons())
                .containsExactly("departure_missing", "changes_missing");
        assertThat(result.factualTravelInfo().extractions().get(4).failureReasons())
                .containsExactly("departure_missing");
        assertThat(result.factualTravelInfo().syntactic().clusters()).hasSize(1);
        assertThat(result.literal().responseCount()).isEqualTo(5);
    }

    private static Analyzer analyzer() {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                (texts, config) -> {
                    throw new AssertionError("Factual travel info analysis must not call the embedding service.");
                },
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new RouteAnalyzer(new RouteStationExtractor()),
                new FactualTravelInfoAnalyzer(),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                new FixedClock(),
                AnalysisConfig::defaults
        );
    }

    private static AnalysisConfig factualConfig() {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                defaults.embeddingProvider(),
                defaults.embeddingBaseUrl(),
                defaults.embeddingModel(),
                defaults.embeddingPrefix(),
                defaults.maxEmbeddingTokens(),
                defaults.semanticDistanceMethod(),
                defaults.semanticRepresentation(),
                defaults.chunk(),
                defaults.distance(),
                ClusteringAlgorithm.FACTUAL_TRAVEL_INFO,
                defaults.scanIncrement(),
                defaults.dbscan(),
                defaults.hierarchical(),
                defaults.route(),
                new FactualTravelInfoConfig("08:02", "09:15", 0),
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
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
