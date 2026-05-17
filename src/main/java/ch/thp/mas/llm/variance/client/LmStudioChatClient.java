package ch.thp.mas.llm.variance.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class LmStudioChatClient implements LlmClient {

    private final String baseUrl;
    private final String apiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LmStudioChatClient(String baseUrl, String apiToken) {
        this(baseUrl, apiToken, HttpClient.newHttpClient(), new ObjectMapper());
    }

    LmStudioChatClient(String baseUrl, String apiToken, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiToken = apiToken;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", config.model());
        request.put("input", prompt);
        request.put("store", false);
        request.put("reasoning", config.reasoning() == null || config.reasoning().isBlank()
                ? "off"
                : config.reasoning());
        if (config.temperature() != null) {
            request.put("temperature", config.temperature());
        }
        if (config.topP() != null) {
            request.put("top_p", config.topP());
        }
        if (config.topK() != null) {
            request.put("top_k", config.topK());
        }

        JsonNode response = post("/api/v1/chat", request);
        List<String> messages = new ArrayList<>();
        for (JsonNode item : response.path("output")) {
            if ("message".equals(item.path("type").asText())) {
                String content = item.path("content").asText();
                if (!content.isBlank()) {
                    messages.add(content);
                }
            }
        }
        if (messages.isEmpty()) {
            throw new IllegalStateException("LM Studio chat response did not contain a message output.");
        }

        JsonNode stats = response.path("stats");
        Long inputTokens = longValue(stats, "input_tokens");
        Long outputTokens = longValue(stats, "total_output_tokens");
        return new LlmResponse(
                String.join("\n", messages).trim(),
                TokenUsage.of(inputTokens, outputTokens),
                textValue(response, "model_instance_id")
        );
    }

    private JsonNode post(String path, JsonNode body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        addAuthorization(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(path + " failed with HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private void addAuthorization(HttpRequest.Builder builder) {
        if (apiToken != null && !apiToken.isBlank()) {
            builder.header("Authorization", "Bearer " + apiToken);
        }
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
