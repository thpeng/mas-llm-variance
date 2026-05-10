package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.semantic.BertScore;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreResult;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class BertScoreHttpService implements BertScoreService {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BertScoreHttpService(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public BertScoreResult score(List<String> texts, AnalysisConfig config) {
        boolean loaded = false;
        try {
            post("/load", loadBody(config));
            loaded = true;
            List<PairRequest> pairs = pairs(texts);
            ScoreResponse response = scorePairs(pairs, config);
            return toResult(response, pairs, texts.size());
        } finally {
            if (loaded) {
                post("/unload", loadBody(config));
            }
        }
    }

    private String loadBody(AnalysisConfig config) {
        try {
            return objectMapper.writeValueAsString(new LoadRequest("bertscore", config.bertScoreModel()));
        } catch (IOException e) {
            throw new AnalysisException("Could not serialize BERTScore service request.", e);
        }
    }

    private ScoreResponse scorePairs(List<PairRequest> pairs, AnalysisConfig config) {
        try {
            String body = objectMapper.writeValueAsString(new ScoreRequest(config.bertScoreModel(), pairs));
            HttpResponse<String> response = post("/score", body);
            return objectMapper.readValue(response.body(), ScoreResponse.class);
        } catch (IOException e) {
            throw new AnalysisException("Could not parse BERTScore service response.", e);
        }
    }

    private BertScoreResult toResult(ScoreResponse response, List<PairRequest> pairs, int textCount) {
        if (response.count() != pairs.size()) {
            throw new AnalysisException("BERTScore service returned count " + response.count()
                    + " but " + pairs.size() + " pairs were requested.");
        }
        if (response.scores() == null) {
            throw new AnalysisException("BERTScore service returned no scores.");
        }
        if (response.scores().size() != pairs.size()) {
            throw new AnalysisException("BERTScore service returned " + response.scores().size()
                    + " scores but " + pairs.size() + " pairs were requested.");
        }

        double[][] distances = new double[textCount][textCount];
        List<BertScoreResult.PairScore> pairScores = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            PairRequest pair = pairs.get(i);
            ScoreDto score = response.scores().get(i);
            if (score.precision() == null || score.recall() == null || score.f1() == null) {
                throw new AnalysisException("BERTScore service returned an incomplete score at index " + i + ".");
            }
            BertScore bertScore = new BertScore(score.precision(), score.recall(), score.f1());
            double distance = 1.0 - bertScore.f1();
            distances[pair.leftIndex()][pair.rightIndex()] = distance;
            distances[pair.rightIndex()][pair.leftIndex()] = distance;
            pairScores.add(new BertScoreResult.PairScore(pair.leftIndex(), pair.rightIndex(), bertScore));
        }
        return new BertScoreResult(distances, pairScores);
    }

    private List<PairRequest> pairs(List<String> texts) {
        List<PairRequest> pairs = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            for (int j = i + 1; j < texts.size(); j++) {
                pairs.add(new PairRequest(i, j, texts.get(i), texts.get(j)));
            }
        }
        return pairs;
    }

    private HttpResponse<String> post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null || body.isBlank() ? "{}" : body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AnalysisException("BERTScore service " + path + " failed with HTTP "
                        + response.statusCode() + ": " + response.body());
            }
            return response;
        } catch (IOException e) {
            throw new AnalysisException("Could not reach BERTScore service at " + baseUrl + path + ".", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisException("Interrupted while calling BERTScore service at " + baseUrl + path + ".", e);
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record LoadRequest(String mode, String model) {
    }

    private record ScoreRequest(String model, List<PairRequest> pairs) {
    }

    private record PairRequest(int leftIndex, int rightIndex, String candidate, String reference) {
    }

    private record ScoreResponse(String model, int count, List<ScoreDto> scores) {
    }

    private record ScoreDto(Double precision, Double recall, Double f1) {
    }
}
