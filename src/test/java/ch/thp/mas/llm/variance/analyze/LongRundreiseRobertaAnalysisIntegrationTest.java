package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreResult;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreService;
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
import ch.thp.mas.llm.variance.analyze.semantic.SemanticDistanceMethod;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.Manufacturer;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LongRundreiseRobertaAnalysisIntegrationTest {

    private static final String EMBEDDINGS_DIR = "/analyze/integration/long/";
    private static final String ANSWER_SEPARATOR = "\n\n===== ANSWER =====\n\n";
    private static final String BERTSCORE_BASE_URL = "http://localhost:8000";
    private static final Map<String, BertScoreResult> BERTSCORE_CACHE = new LinkedHashMap<>();

    @ParameterizedTest(name = "{0} live xlm-roberta BERTScore analyzes with epsilon {2}")
    @CsvSource({
            "qwen3-8b, qwen3-8b-answers.txt, 0.15",
            "qwen3-8b, qwen3-8b-answers.txt, 0.08",
            "qwen3-8b, qwen3-8b-answers.txt, 0.05",
            "sonnet45, sonnet45-answers.txt, 0.15",
            "sonnet45, sonnet45-answers.txt, 0.08",
            "sonnet45, sonnet45-answers.txt, 0.05"
    })
    void analyzesLongModelOutputsWithLiveXlmRobertaBertScore(
            String name,
            String answersFilename,
            double epsilon
    ) throws Exception {
        List<String> responses = loadAnswers(answersFilename);

        AnalysisResult result = analyzer(
                name,
                responses,
                configWithEpsilon(epsilon)
        ).analyze(new NamedRunLog(name + "-long-rundreise-roberta.json", runLog(name, responses)));

        assertThat(result.config().semanticDistanceMethod()).isEqualTo(SemanticDistanceMethod.BERTSCORE_F1);
        assertThat(result.semantic().responseCount()).isEqualTo(responses.size());
        assertThat(result.semantic().truncatedResponses()).isZero();
        assertThat(result.semantic().pairwiseCosineDistance().count())
                .isEqualTo(responses.size() * (responses.size() - 1) / 2);
        assertThat(result.semantic().pairwiseCosineDistance().min()).isBetween(0.0, 1.0);
        assertThat(result.semantic().pairwiseCosineDistance().max()).isBetween(0.0, 1.0);
        assertThat(result.semantic().clusters()).isNotEmpty();
        assertThat(result.semantic().clusters().stream().mapToInt(cluster -> cluster.size()).sum()
                + result.semantic().outliers().size()).isEqualTo(responses.size());
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

    private static InputStream resource(String name) {
        InputStream inputStream = LongRundreiseRobertaAnalysisIntegrationTest.class.getResourceAsStream(name);
        return Objects.requireNonNull(inputStream, "Missing test resource: " + name);
    }

    private static Analyzer analyzer(String name, List<String> responses, AnalysisConfig config) {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                (texts, ignored) -> {
                    throw new AssertionError("Embedding service must not be called for BERTScore analysis.");
                },
                new CachedBertScoreService(name, responses),
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new SummaryStatistics(),
                new FixedClock(),
                () -> config
        );
    }

    private static AnalysisConfig configWithEpsilon(double epsilon) {
        return new AnalysisConfig(
                "e5-http",
                BERTSCORE_BASE_URL,
                "intfloat/multilingual-e5-large",
                "passage:",
                514,
                SemanticDistanceMethod.BERTSCORE_F1,
                BERTSCORE_BASE_URL,
                "xlm-roberta-large",
                SemanticRepresentation.FULL_TEXT,
                new ChunkConfig(120),
                DistanceMetric.COSINE,
                ClusteringAlgorithm.DBSCAN,
                new DbscanConfig(epsilon, 2),
                new HierarchicalConfig(0.08, HierarchicalLinkage.COMPLETE),
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
                name.startsWith("sonnet") ? Manufacturer.ANTHROPIC : Manufacturer.LMSTUDIO,
                name,
                "xlm-roberta-large-bertscore",
                responses.size(),
                new RunConfigLog(0.0, null, null, null),
                "Long fixture run for semantic route variance.",
                entries
        );
    }

    private record CachedBertScoreService(String name, List<String> expectedResponses) implements BertScoreService {

        @Override
        public BertScoreResult score(List<String> texts, AnalysisConfig config) {
            if (!texts.equals(expectedResponses)) {
                throw new AnalysisException("Unexpected responses for cached BERTScore fixture: " + name);
            }
            return BERTSCORE_CACHE.computeIfAbsent(name, ignored -> new BertScoreHttpService(
                    config.bertScoreBaseUrl(),
                    HttpClient.newHttpClient(),
                    new ObjectMapper()
            ).score(texts, config));
        }
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-08T13:00:00+02:00");
        }
    }
}
