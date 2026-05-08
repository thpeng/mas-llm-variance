package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.thp.mas.llm.variance.client.Manufacturer;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LongRundreiseE5AnalysisIntegrationTest {

    private static final String EMBEDDINGS_DIR = "/analyze/integration/long/";
    private static final String ANSWER_SEPARATOR = "\n\n===== ANSWER =====\n\n";

    @ParameterizedTest(name = "{0} fixture contains {2} model answers")
    @CsvSource({
            "qwen3-8b, qwen3-8b-answers.txt, 50",
            "sonnet45, sonnet45-answers.txt, 20"
    })
    void longAnswerFixtureContainsIndividualAnswers(
            String name,
            String answersFilename,
            int expectedCount
    ) throws Exception {
        List<String> responses = loadAnswers(answersFilename);

        assertThat(responses).hasSize(expectedCount);
        assertThat(responses).allSatisfy(response -> assertThat(response)
                .contains("Rundreise")
                .doesNotContain("Process finished with exit code"));
    }

    @ParameterizedTest(name = "{0} captured E5 embeddings analyze with epsilon {3}")
    @CsvSource({
            "qwen3-8b, qwen3-8b-answers.txt, qwen3-8b-e5-embedding-response.json, 0.01",
            "qwen3-8b, qwen3-8b-answers.txt, qwen3-8b-e5-embedding-response.json, 0.08",
            "qwen3-8b, qwen3-8b-answers.txt, qwen3-8b-e5-embedding-response.json, 0.05",
            "sonnet45, sonnet45-answers.txt, sonnet45-e5-embedding-response.json, 0.15",
            "sonnet45, sonnet45-answers.txt, sonnet45-e5-embedding-response.json, 0.08",
            "sonnet45, sonnet45-answers.txt, sonnet45-e5-embedding-response.json, 0.01"
    })
    void analyzesLongModelOutputsWithCapturedMultilingualE5Embeddings(
            String name,
            String answersFilename,
            String embeddingFilename,
            double epsilon
    ) throws Exception {
        List<String> responses = loadAnswers(answersFilename);
        CapturedE5Response capturedResponse = loadCapturedE5Response(embeddingFilename);
        assumeTrue(capturedResponse.hasRealEmbeddingsFor(responses.size()),
                () -> "Embedding fixture " + EMBEDDINGS_DIR + embeddingFilename
                        + " has count=" + capturedResponse.count()
                        + ", dim=" + capturedResponse.dim()
                        + ", embeddings=" + capturedResponse.embeddingCount()
                        + ", but expected " + responses.size() + " embeddings.");

        AnalysisResult result = analyzer(new CapturedEmbeddingService(capturedResponse), configWithEpsilon(epsilon))
                .analyze(new NamedRunLog(name + "-long-rundreise-e5.json", runLog(name, responses)));

        assertThat(result.semantic().responseCount()).isEqualTo(responses.size());
        assertThat(result.semantic().pairwiseCosineDistance().count())
                .isEqualTo(responses.size() * (responses.size() - 1) / 2);
        assertThat(result.semantic().clusters()).isNotEmpty();
        assertThat(result.semantic().medoid().response()).isIn(responses);
        assertThat(result.syntactic().clusters()).hasSameSizeAs(result.semantic().clusters());
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

    private static CapturedE5Response loadCapturedE5Response(String embeddingFilename) throws IOException {
        try (InputStream inputStream = resource(EMBEDDINGS_DIR + embeddingFilename)) {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return CapturedE5Response.empty();
            }
            try {
                return new ObjectMapper().readValue(json, CapturedE5Response.class);
            } catch (IOException ignored) {
                return CapturedE5Response.empty();
            }
        }
    }

    private static InputStream resource(String name) {
        InputStream inputStream = LongRundreiseE5AnalysisIntegrationTest.class.getResourceAsStream(name);
        return Objects.requireNonNull(inputStream, "Missing test resource: " + name);
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
                defaults.semanticRepresentation(),
                defaults.chunk(),
                defaults.distance(),
                defaults.clusteringAlgorithm(),
                new DbscanConfig(epsilon, defaults.dbscan().minPts()),
                defaults.hierarchical(),
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
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
                name.startsWith("sonnet") ? Manufacturer.ANTHROPIC : Manufacturer.LMSTUDIO,
                name,
                null,
                responses.size(),
                new RunConfigLog(0.0, null, null, null),
                "Long fixture run for semantic route variance.",
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

        private static CapturedE5Response empty() {
            return new CapturedE5Response(0, 0, List.of());
        }

        private boolean hasRealEmbeddingsFor(int expectedCount) {
            if (dim <= 0 || count != expectedCount || embeddings == null || embeddings.size() != expectedCount) {
                return false;
            }
            return embeddings.stream().allMatch(vector -> vector != null && vector.size() == dim);
        }

        private int embeddingCount() {
            return embeddings == null ? 0 : embeddings.size();
        }
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-08T13:00:00+02:00");
        }
    }
}
