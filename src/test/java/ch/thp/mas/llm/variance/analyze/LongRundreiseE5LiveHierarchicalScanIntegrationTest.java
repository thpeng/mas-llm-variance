package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.DistanceMetric;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.semantic.ScanRange;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LongRundreiseE5LiveHierarchicalScanIntegrationTest {

    private static final String EMBEDDINGS_DIR = "/analyze/integration/long/";
    private static final String ANSWER_SEPARATOR = "\n\n===== ANSWER =====\n\n";
    private static final String E5_BASE_URL = "http://localhost:8000";
    private static final Map<String, List<EmbeddingResult>> EMBEDDING_CACHE = new LinkedHashMap<>();

    @ParameterizedTest(name = "{0} live E5 chunk hierarchical threshold {2}")
    @CsvSource({
            "qwen3-8b, qwen3-8b-answers.txt, 0.01",
            "qwen3-8b, qwen3-8b-answers.txt, 0.03",
            "qwen3-8b, qwen3-8b-answers.txt, 0.05",
            "qwen3-8b, qwen3-8b-answers.txt, 0.08",
            "qwen3-8b, qwen3-8b-answers.txt, 0.12",
            "sonnet45, sonnet45-answers.txt, 0.01",
            "sonnet45, sonnet45-answers.txt, 0.03",
            "sonnet45, sonnet45-answers.txt, 0.05",
            "sonnet45, sonnet45-answers.txt, 0.08",
            "sonnet45, sonnet45-answers.txt, 0.12"
    })
    void scansLongModelOutputsWithLiveE5ChunkHierarchical(
            String name,
            String answersFilename,
            double threshold
    ) throws Exception {
        assumeTrue(e5ServerAvailable(), "E5 server is not reachable at " + E5_BASE_URL);
        List<String> responses = loadAnswers(answersFilename);

        AnalysisResult result = analyzer(name, configWithThreshold(threshold))
                .analyze(new NamedRunLog(name + "-long-rundreise-e5-live-hierarchical.json", runLog(name, responses)));
        AnalysisScan scan = result.scans().getFirst();

        System.out.println(name
                + " threshold=" + threshold
                + " clusters=" + scan.semantic().clusters().size()
                + " outliers=" + scan.semantic().outliers().size()
                + " medianDistance=" + scan.semantic().pairwiseCosineDistance().median()
                + " maxDistance=" + scan.semantic().pairwiseCosineDistance().max());

        assertThat(result.config().semanticRepresentation()).isEqualTo(SemanticRepresentation.CHUNK_AVERAGE_MIN);
        assertThat(result.config().clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.HIERARCHICAL);
        assertThat(scan.semantic().responseCount()).isEqualTo(responses.size());
        assertThat(scan.semantic().clusters()).isNotEmpty();
        assertThat(scan.semantic().clusters().stream().mapToInt(cluster -> cluster.size()).sum()
                + scan.semantic().outliers().size()).isEqualTo(responses.size());
        assertThat(scan.syntactic().clusters()).hasSameSizeAs(scan.semantic().clusters());
    }

    private static List<String> loadAnswers(String answersFilename) throws IOException {
        try (InputStream inputStream = resource(EMBEDDINGS_DIR + answersFilename)) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return java.util.Arrays.stream(content.split(ANSWER_SEPARATOR))
                    .map(String::trim)
                    .filter(answer -> !answer.isBlank())
                    .toList();
        }
    }

    private static InputStream resource(String name) {
        InputStream inputStream = LongRundreiseE5LiveHierarchicalScanIntegrationTest.class.getResourceAsStream(name);
        return Objects.requireNonNull(inputStream, "Missing test resource: " + name);
    }

    private static boolean e5ServerAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(E5_BASE_URL + "/load"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            int status = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            return status >= 200 && status < 300;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static Analyzer analyzer(String name, AnalysisConfig config) {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                new CachedEmbeddingService(name),
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new RouteAnalyzer(new RouteStationExtractor()),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                new FixedClock(),
                () -> config
        );
    }

    private static AnalysisConfig configWithThreshold(double threshold) {
        return new AnalysisConfig(
                "e5-http",
                E5_BASE_URL,
                "intfloat/multilingual-e5-large",
                "passage:",
                514,
                AnalysisConfig.defaults().semanticDistanceMethod(),
                SemanticRepresentation.CHUNK_AVERAGE_MIN,
                new ChunkConfig(1),
                DistanceMetric.COSINE,
                ClusteringAlgorithm.HIERARCHICAL,
                0.01,
                new DbscanConfig(ScanRange.ofHundredths(15, 15), 2),
                new HierarchicalConfig(ScanRange.of(threshold, threshold, 0.01, "analysis.hierarchical.threshold"),
                        HierarchicalLinkage.COMPLETE),
                AnalysisConfig.defaults().route(),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1),
                PercentileMethod.NEAREST_RANK
        );
    }

    private static RunLog runLog(String name, List<String> responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-08T12:00:00+02:00");
        List<RunLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            entries.add(new RunLogEntry(i + 1, now, now, responses.get(i), null));
        }
        return new RunLog(
                "0001-rundreise-schweiz-" + name,
                now,
                now,
                name.startsWith("sonnet") ? InferenceProvider.ANTHROPIC : InferenceProvider.LMSTUDIO,
                name,
                "intfloat/multilingual-e5-large-live",
                null,
                responses.size(),
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "Long fixture run for live E5 hierarchical scan.",
                entries
        );
    }

    private record CachedEmbeddingService(String name) implements EmbeddingService {

        @Override
        public List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config) {
            String key = name + ":" + texts.hashCode();
            return EMBEDDING_CACHE.computeIfAbsent(key, ignored -> new E5HttpEmbeddingService(
                    config.embeddingBaseUrl(),
                    HttpClient.newHttpClient(),
                    new ObjectMapper()
            ).embed(texts, config));
        }
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-08T13:00:00+02:00");
        }
    }
}
