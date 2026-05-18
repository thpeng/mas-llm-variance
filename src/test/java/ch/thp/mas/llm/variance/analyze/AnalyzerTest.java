package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyzerTest {

    @Test
    void analyzesRunWithFakeEmbeddings() {
        Analyzer analyzer = analyzer(
                (texts, config) -> List.of(
                        new EmbeddingResult(new double[]{1, 0}, false),
                        new EmbeddingResult(new double[]{0.99, 0.01}, false),
                        new EmbeddingResult(new double[]{0, 1}, false)
                )
        );

        AnalysisResult result = analyzer.analyze(new NamedRunLog("run.json", runLog()));

        assertThat(result.sourceRun()).isEqualTo("run.json");
        assertThat(result.semantic().responseCount()).isEqualTo(3);
        assertThat(result.config().clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.HIERARCHICAL);
        assertThat(result.semantic().clusters()).hasSize(2);
        assertThat(result.semantic().clusters().getFirst().repetitionIndices()).containsExactly(1, 2);
        assertThat(result.semantic().clusters().get(1).repetitionIndices()).containsExactly(3);
        assertThat(result.semantic().outliers()).isEmpty();
        assertThat(result.syntactic().clusters().getFirst().pairCount()).isEqualTo(1);
        assertThat(result.literal().responseCount()).isEqualTo(3);
        assertThat(result.literal().clusters().getFirst().pairCount()).isEqualTo(1);
    }

    @Test
    void analyzesStableSingleWordAnswersExactly() {
        Analyzer analyzer = analyzer((texts, config) -> List.of(
                new EmbeddingResult(new double[]{1, 0}, false),
                new EmbeddingResult(new double[]{1, 0}, false),
                new EmbeddingResult(new double[]{1, 0}, false)
        ));

        AnalysisResult result = analyzer.analyze(new NamedRunLog("single-word-capital.json", stableRunLog()));

        assertThat(result.semantic().clusters()).hasSize(1);
        assertThat(result.semantic().outliers()).isEmpty();
        assertThat(result.semantic().medoid().repetitionIndex()).isEqualTo(1);
        assertThat(result.semantic().pairwiseCosineDistance().max()).isEqualTo(0.0);
        assertThat(result.syntactic().clusters().getFirst().pairCount()).isEqualTo(3);
        assertThat(result.syntactic().clusters().getFirst().rougeLDistance().median()).isEqualTo(0.0);
        assertThat(result.syntactic().clusters().getFirst().bleuDistance().median()).isEqualTo(0.0);
        assertThat(result.literal().allResponsesIdentical()).isTrue();
        assertThat(result.literal().exactMatchRate()).isEqualTo(1.0);
        assertThat(result.literal().clusters().getFirst().exactMatchRate()).isEqualTo(1.0);
    }

    @Test
    void analyzesTwoClearSemanticClusters() {
        Analyzer analyzer = analyzer((texts, config) -> List.of(
                new EmbeddingResult(new double[]{1.0, 0.0}, false),
                new EmbeddingResult(new double[]{0.99, 0.01}, false),
                new EmbeddingResult(new double[]{0.0, 1.0}, false),
                new EmbeddingResult(new double[]{0.01, 0.99}, false)
        ));

        AnalysisResult result = analyzer.analyze(new NamedRunLog("two-clusters.json", runLog(
                "Bern ist die Hauptstadt.",
                "Die Hauptstadt ist Bern.",
                "Die Reise startet in Genf.",
                "Eine Rundreise startet in Genf."
        )));

        assertThat(result.semantic().clusters()).hasSize(2);
        assertThat(result.semantic().clusters().get(0).repetitionIndices()).containsExactly(1, 2);
        assertThat(result.semantic().clusters().get(1).repetitionIndices()).containsExactly(3, 4);
        assertThat(result.semantic().outliers()).isEmpty();
        assertThat(result.syntactic().clusters()).extracting(SyntacticCluster::pairCount)
                .containsExactly(1, 1);
        assertThat(result.literal().allResponsesIdentical()).isFalse();
        assertThat(result.literal().clusters()).extracting(cluster -> cluster.exactMatchRate())
                .containsExactly(0.0, 0.0);
    }

    @Test
    void reportsTruncatedResponses() {
        Analyzer analyzer = analyzer((texts, config) -> List.of(
                new EmbeddingResult(new double[]{1, 0}, true),
                new EmbeddingResult(new double[]{1, 0}, false)
        ));

        AnalysisResult result = analyzer.analyze(new NamedRunLog("truncated.json", runLog("Bern", "Bern")));

        assertThat(result.semantic().truncatedResponses()).isEqualTo(1);
    }

    @Test
    void analyzesChunkAverageMinDistancesWithHierarchicalClustering() {
        AnalysisConfig config = config(
                SemanticRepresentation.CHUNK_AVERAGE_MIN,
                ClusteringAlgorithm.HIERARCHICAL,
                new HierarchicalConfig(0.1, HierarchicalLinkage.COMPLETE)
        );
        Analyzer analyzer = analyzer((texts, ignored) -> texts.stream()
                .map(text -> {
                    if (text.contains("Basel")) {
                        return new EmbeddingResult(new double[]{0, 1}, false);
                    }
                    if (text.contains("Lausanne")) {
                        return new EmbeddingResult(new double[]{-1, 0}, false);
                    }
                    return new EmbeddingResult(new double[]{1, 0}, false);
                })
                .toList(), config);

        AnalysisResult result = analyzer.analyze(new NamedRunLog("chunked.json", runLog(
                "Gemeinsame Einleitung.\n\nBasel als Abschluss.",
                "Gemeinsame Einleitung.\n\nLausanne als Abschluss.",
                "Gemeinsame Einleitung.\n\nBasel als Abschluss."
        )));

        assertThat(result.config().semanticRepresentation()).isEqualTo(SemanticRepresentation.CHUNK_AVERAGE_MIN);
        assertThat(result.config().clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.HIERARCHICAL);
        assertThat(result.semantic().clusters()).hasSize(2);
        assertThat(result.semantic().clusters().get(0).repetitionIndices()).containsExactly(1, 3);
        assertThat(result.semantic().clusters().get(1).repetitionIndices()).containsExactly(2);
        assertThat(result.semantic().outliers()).isEmpty();
    }

    @Test
    void rejectsRunWithoutResponses() {
        Analyzer analyzer = analyzer((texts, config) -> List.of());
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        RunLog empty = new RunLog(
                "0001-empty",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                0,
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "prompt",
                List.of()
        );

        assertThatThrownBy(() -> analyzer.analyze(new NamedRunLog("empty.json", empty)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no responses");
    }

    @Test
    void goldenStableSingleWordFixtureMatchesExpectedJson() throws Exception {
        Analyzer analyzer = analyzer((texts, config) -> List.of(
                new EmbeddingResult(new double[]{1, 0}, false),
                new EmbeddingResult(new double[]{1, 0}, false),
                new EmbeddingResult(new double[]{1, 0}, false)
        ));

        AnalysisResult result = analyzer.analyze(new NamedRunLog("single-word-capital.json", stableRunLog()));

        ObjectMapper objectMapper = objectMapper();
        JsonNode actual = objectMapper.valueToTree(result);
        JsonNode expected;
        try (InputStream inputStream = getClass().getResourceAsStream("/analyze/expected/single-word-capital-analysis.json")) {
            expected = objectMapper.readTree(inputStream);
        }
        assertThat(actual).isEqualTo(expected);
    }

    private static RunLog runLog() {
        return runLog(
                "Die Hauptstadt ist Bern.",
                "Bern ist die Hauptstadt.",
                "Eine Rundreise durch die Schweiz."
        );
    }

    private static RunLog stableRunLog() {
        return runLog("Bern", "Bern", "Bern");
    }

    private static RunLog runLog(String... responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new RunLog(
                "0001-test",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                responses.length,
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "prompt",
                java.util.stream.IntStream.range(0, responses.length)
                        .mapToObj(index -> new RunLogEntry(index + 1, now, now, responses[index], null))
                        .toList()
        );
    }

    private static Analyzer analyzer(EmbeddingService embeddingService) {
        return analyzer(embeddingService, AnalysisConfig.defaults());
    }

    private static Analyzer analyzer(EmbeddingService embeddingService, AnalysisConfig config) {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                embeddingService,
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                new FixedClock(),
                () -> config
        );
    }

    private static AnalysisConfig config(
            SemanticRepresentation semanticRepresentation,
            ClusteringAlgorithm clusteringAlgorithm,
            HierarchicalConfig hierarchicalConfig
    ) {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                defaults.embeddingProvider(),
                defaults.embeddingBaseUrl(),
                defaults.embeddingModel(),
                defaults.embeddingPrefix(),
                defaults.maxEmbeddingTokens(),
                defaults.semanticDistanceMethod(),
                semanticRepresentation,
                new ChunkConfig(4),
                defaults.distance(),
                clusteringAlgorithm,
                defaults.dbscan(),
                hierarchicalConfig,
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-02T11:00:00+02:00");
        }
    }
}
