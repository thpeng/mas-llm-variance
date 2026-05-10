package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.semantic.BertScoreResult;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "runLiveDiagnostics", matches = "true")
class LongRundreiseRobertaDiagnosticsTest {

    private static final String EMBEDDINGS_DIR = "/analyze/integration/long/";
    private static final String ANSWER_SEPARATOR = "\n\n===== ANSWER =====\n\n";

    @Test
    void printDistanceRanges() throws Exception {
        print("qwen3-8b", "qwen3-8b-answers.txt");
        print("sonnet45", "sonnet45-answers.txt");
    }

    private void print(String name, String answersFilename) throws Exception {
        List<String> responses = loadAnswers(answersFilename);
        BertScoreService service = new BertScoreHttpService(
                "http://localhost:8000",
                HttpClient.newHttpClient(),
                new ObjectMapper()
        );
        BertScoreResult result = service.score(responses, AnalysisConfig.defaults());
        double[] distances = upperTriangle(result.distances());
        Arrays.sort(distances);
        System.out.println(name + " count=" + distances.length
                + " min=" + distances[0]
                + " p10=" + percentile(distances, 0.10)
                + " p25=" + percentile(distances, 0.25)
                + " median=" + percentile(distances, 0.50)
                + " p75=" + percentile(distances, 0.75)
                + " p90=" + percentile(distances, 0.90)
                + " max=" + distances[distances.length - 1]);
    }

    private static List<String> loadAnswers(String answersFilename) throws Exception {
        try (InputStream inputStream = LongRundreiseRobertaDiagnosticsTest.class
                .getResourceAsStream(EMBEDDINGS_DIR + answersFilename)) {
            String content = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(content.split(ANSWER_SEPARATOR))
                    .map(String::trim)
                    .filter(answer -> !answer.isBlank())
                    .toList();
        }
    }

    private static double[] upperTriangle(double[][] distances) {
        int size = distances.length;
        double[] values = new double[size * (size - 1) / 2];
        int offset = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                values[offset++] = distances[i][j];
            }
        }
        return values;
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = Math.min(sorted.length - 1, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }
}
