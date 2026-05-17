package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MultilingualE5AnalysisIntegrationTest {

    private static final String RESPONSES_RESOURCE = "/analyze/integration/rundreise-responses.txt";
    private static final String EMBEDDINGS_RESOURCE = "/analyze/integration/rundreise-e5-embedding-response.json";

    @Test
    void analyzesRundreiseResponsesWithCapturedMultilingualE5Embeddings() throws Exception {
        List<String> responses = loadResponses();
        CapturedE5Response capturedResponse = loadCapturedE5Response();
        assumeTrue(capturedResponse.hasRealEmbeddingsFor(responses.size()),
                "Paste the real /embed JSON into " + EMBEDDINGS_RESOURCE + " to run this integration harness.");

        AnalysisResult result = analyzer(new CapturedEmbeddingService(capturedResponse))
                .analyze(new NamedRunLog("rundreise-captured-e5.json", runLog(responses)));

        assertThat(result.semantic().responseCount()).isEqualTo(responses.size());
        assertThat(result.semantic().pairwiseCosineDistance().count()).isEqualTo(28);
        assertThat(result.semantic().clusters()).isNotEmpty();
        assertThat(result.semantic().medoid().response()).isIn(responses);
        assertThat(result.semantic().clusters())
                .allSatisfy(cluster -> assertThat(cluster.repetitionIndices())
                        .allSatisfy(index -> assertThat(index).isBetween(1, responses.size())));
        assertThat(result.syntactic().clusters())
                .hasSameSizeAs(result.semantic().clusters());
    }

    @ParameterizedTest(name = "epsilon {0} produces {1} semantic cluster(s) and outliers {2}")
    @CsvSource({
            "0.15, 1, ''",
            "0.12, 1, ''",
            "0.10, 1, '6'",
            "0.08, 1, '6'",
            "0.06, 2, '6,7,8'",
            "0.05, 2, '6,7,8'"
    })
    void analyzesCapturedMultilingualE5EmbeddingsWithConfigurableEpsilon(
            double epsilon,
            int expectedClusterCount,
            String expectedOutlierCsv
    ) throws Exception {
        List<String> responses = loadResponses();
        CapturedE5Response capturedResponse = loadCapturedE5Response();
        assumeTrue(capturedResponse.hasRealEmbeddingsFor(responses.size()),
                "Paste the real /embed JSON into " + EMBEDDINGS_RESOURCE + " to run this integration harness.");

        AnalysisResult result = analyzer(new CapturedEmbeddingService(capturedResponse), configWithEpsilon(epsilon))
                .analyze(new NamedRunLog("rundreise-captured-e5.json", runLog(responses)));

        assertThat(result.config().clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.DBSCAN);
        assertThat(result.config().dbscan().epsilon()).isEqualTo(epsilon);
        assertThat(result.semantic().clusters()).hasSize(expectedClusterCount);
        assertThat(result.semantic().outliers()).containsExactlyElementsOf(outliers(expectedOutlierCsv));
    }

    private static List<String> loadResponses() throws IOException {
        String content;
        try (InputStream inputStream = resource(RESPONSES_RESOURCE)) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static CapturedE5Response loadCapturedE5Response() throws IOException {
        try (InputStream inputStream = resource(EMBEDDINGS_RESOURCE)) {
            return new ObjectMapper().readValue(inputStream, CapturedE5Response.class);
        }
    }

    private static InputStream resource(String name) {
        InputStream inputStream = MultilingualE5AnalysisIntegrationTest.class.getResourceAsStream(name);
        return Objects.requireNonNull(inputStream, "Missing test resource: " + name);
    }

    private static Analyzer analyzer(EmbeddingService embeddingService) {
        return analyzer(embeddingService, AnalysisConfig.defaults());
    }

    private static Analyzer analyzer(EmbeddingService embeddingService, AnalysisConfig config) {
        TextTokenizer tokenizer = new TextTokenizer();
        return new Analyzer(
                embeddingService,
                new CosineDistance(),
                new MedoidSelector(),
                new DbscanClusterer(),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new SummaryStatistics(),
                new FixedClock(),
                () -> config
        );
    }

    private static AnalysisConfig configWithEpsilon(double epsilon) {
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
                ClusteringAlgorithm.DBSCAN,
                new DbscanConfig(epsilon, defaults.dbscan().minPts()),
                defaults.hierarchical(),
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
        );
    }

    private static List<Integer> outliers(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .toList();
    }

    private static RunLog runLog(List<String> responses) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-08T10:00:00+02:00");
        List<RunLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            entries.add(new RunLogEntry(i + 1, now, now, responses.get(i), null));
        }
        return new RunLog(
                "0001-rundreise-schweiz",
                now,
                now,
                InferenceProvider.OPENAI,
                "captured-fixture",
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, null, null, null, "off"),
                "Fixture run for semantic route variance.",
                entries
        );
    }

    private record CapturedEmbeddingService(CapturedE5Response response) implements EmbeddingService {

        @Override
        public List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config) {
            if (!response.hasRealEmbeddingsFor(texts.size())) {
                throw new AnalysisException("Captured E5 response does not match response fixture size.");
            }
            return response.embeddings().stream()
                    .map(vector -> new EmbeddingResult(vector.stream().mapToDouble(Double::doubleValue).toArray(), false))
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CapturedE5Response(int dim, int count, List<List<Double>> embeddings) {

        private boolean hasRealEmbeddingsFor(int expectedCount) {
            if (dim <= 0 || count != expectedCount || embeddings == null || embeddings.size() != expectedCount) {
                return false;
            }
            return embeddings.stream().allMatch(vector -> vector != null && vector.size() == dim);
        }
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-08T11:00:00+02:00");
        }
    }
}
