package ch.thp.mas.llm.variance.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeminiClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build(), new ObjectMapper());
    }

    GeminiClient(String apiKey, String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        HttpJsonResponse httpResponse = post(config.model(), request(prompt, config));
        JsonNode response = httpResponse.body();
        String text = responseText(response);
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini response did not contain text output.");
        }
        return new LlmResponse(text.trim(), tokenUsage(response), null, httpResponse.requestTrace());
    }

    private ObjectNode request(String prompt, LlmRequestConfig config) {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.putArray("parts").add(objectMapper.createObjectNode().put("text", prompt));
        request.putArray("contents").add(content);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        if (config.temperature() != null) {
            generationConfig.put("temperature", config.temperature());
        }
        if (config.topP() != null) {
            generationConfig.put("topP", config.topP());
        }
        if (config.topK() != null) {
            generationConfig.put("topK", config.topK());
        }
        if (config.seed() != null) {
            generationConfig.put("seed", Math.toIntExact(config.seed()));
        }
        if (config.sendReasoning() && config.reasoning() != null) {
            generationConfig.set("thinkingConfig", objectMapper.createObjectNode()
                    .put("thinkingLevel", reasoningValue(config)));
        }
        if (!generationConfig.isEmpty()) {
            request.set("generationConfig", generationConfig);
        }
        return request;
    }

    private HttpJsonResponse post(String model, JsonNode body) throws Exception {
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "/models/" + encodedModel + ":generateContent?key=" + encodedApiKey);
        String requestBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(10))
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        RequestTrace requestTrace = RequestTrace.of(request, requestBody, response);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServingException("Gemini generateContent failed for model '" + model + "' with HTTP "
                    + response.statusCode() + ": " + response.body(), response.statusCode(), response.body(),
                    requestTrace);
        }
        return new HttpJsonResponse(objectMapper.readTree(response.body()), requestTrace);
    }

    private static String responseText(JsonNode response) {
        List<String> parts = new ArrayList<>();
        for (JsonNode candidate : response.path("candidates")) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                String text = part.path("text").asText();
                if (!text.isBlank()) {
                    parts.add(text);
                }
            }
        }
        return String.join("\n", parts);
    }

    private static TokenUsage tokenUsage(JsonNode response) {
        JsonNode usage = response.path("usageMetadata");
        Long inputTokens = longValue(usage, "promptTokenCount");
        Long outputTokens = longValue(usage, "candidatesTokenCount");
        Long totalTokens = longValue(usage, "totalTokenCount");
        return new TokenUsage(inputTokens, outputTokens, totalTokens);
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String reasoningValue(LlmRequestConfig config) {
        if (config.reasoningProviderValue() != null && !config.reasoningProviderValue().isBlank()) {
            return config.reasoningProviderValue();
        }
        return config.reasoning().geminiThinkingLevel();
    }

    private record HttpJsonResponse(JsonNode body, RequestTrace requestTrace) {
    }
}
