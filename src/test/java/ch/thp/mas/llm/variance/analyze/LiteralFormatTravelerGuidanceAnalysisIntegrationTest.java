package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalyzer;
import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextAnalyzer;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceAnalyzer;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceClassification;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceConfig;
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

class LiteralFormatTravelerGuidanceAnalysisIntegrationTest {

    private static final String REFERENCE =
            "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.";

    @Test
    void analyzesLiteralFormatTravelerGuidanceWithoutEmbeddingService() {
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

        assertThat(result.scans()).isEmpty();
        assertThat(result.route()).isNull();
        assertThat(result.factualTravelInfo()).isNull();
        assertThat(result.literalFormatTravelerGuidance()).isNotNull();
        assertThat(result.literalFormatTravelerGuidance().responseCount()).isEqualTo(5);
        assertThat(result.literalFormatTravelerGuidance().exactMatchCount()).isEqualTo(1);
        assertThat(result.literalFormatTravelerGuidance().normalizedExactMatchCount()).isEqualTo(1);
        assertThat(result.literalFormatTravelerGuidance().noMatchCount()).isEqualTo(3);
        assertThat(result.literalFormatTravelerGuidance().outliers()).containsExactly(3, 4, 5);
        assertThat(result.literalFormatTravelerGuidance().forbiddenTemplateTermCounts())
                .containsEntry("Sargans", 1)
                .containsEntry("Chur", 1)
                .containsEntry("Landquart", 1)
                .containsEntry("RE5", 1)
                .containsEntry("S12", 1);
        assertThat(result.literalFormatTravelerGuidance().additionalSentenceCandidateCount()).isEqualTo(1);
        assertThat(result.literalFormatTravelerGuidance().extractions())
                .extracting("classification")
                .containsExactly(
                        LiteralFormatTravelerGuidanceClassification.EXACT_MATCH,
                        LiteralFormatTravelerGuidanceClassification.NORMALIZED_EXACT_MATCH,
                        LiteralFormatTravelerGuidanceClassification.NO_MATCH,
                        LiteralFormatTravelerGuidanceClassification.NO_MATCH,
                        LiteralFormatTravelerGuidanceClassification.NO_MATCH
                );
        assertThat(result.literal().responseCount()).isEqualTo(5);
    }

    private static Analyzer analyzer() {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                (texts, config) -> {
                    throw new AssertionError(
                            "Literal format traveler guidance analysis must not call the embedding service.");
                },
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new RouteAnalyzer(new RouteStationExtractor()),
                new FactualTravelInfoAnalyzer(),
                new LiteralFormatTravelerGuidanceAnalyzer(),
                new CreativeMarketingTextAnalyzer(),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                new FixedClock(),
                AnalysisConfig::defaults
        );
    }

    private static AnalysisConfig literalFormatConfig() {
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
                ClusteringAlgorithm.LITERAL_FORMAT_TRAVELER_GUIDANCE,
                defaults.scanIncrement(),
                defaults.dbscan(),
                defaults.hierarchical(),
                defaults.route(),
                defaults.factualTravelInfo(),
                new LiteralFormatTravelerGuidanceConfig(REFERENCE),
                defaults.creativeMarketingText(),
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
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
