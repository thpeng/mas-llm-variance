package ch.thp.mas.llm.variance.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenAiClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build(), new ObjectMapper());
    }

    OpenAiClient(String apiKey, String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        HttpJsonResponse httpResponse = post(request(prompt, config));
        JsonNode response = httpResponse.body();
        String text = responseText(response);
        if (text.isBlank()) {
            text = response.toString();
        }
        return new LlmResponse(text.trim(), tokenUsage(response), null, httpResponse.requestTrace());
    }

    private ObjectNode request(String prompt, LlmRequestConfig config) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", config.model());
        request.put("input", prompt);

        if (sendsSamplingParameters(config) && config.temperature() != null) {
            request.put("temperature", config.temperature());
        }
        if (sendsSamplingParameters(config) && config.topP() != null) {
            request.put("top_p", config.topP());
        }
        if (config.sendReasoning() && config.reasoning() != null && supportsReasoning(config.model())) {
            request.set("reasoning", objectMapper.createObjectNode()
                    .put("effort", reasoningValue(config)));
        } else if (config.sendReasoning() && config.reasoning() != null && config.reasoning() != Reasoning.OFF) {
            throw new IllegalArgumentException("OpenAI model '" + config.model()
                    + "' does not support reasoning. Set reasoning to 'off' or use an OpenAI reasoning model.");
        }
        return request;
    }

    private HttpJsonResponse post(JsonNode body) throws Exception {
        String requestBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/responses"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(10))
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        RequestTrace requestTrace = RequestTrace.of(request, requestBody, response);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServingException("OpenAI responses failed with HTTP "
                    + response.statusCode() + ": " + response.body(), response.statusCode(), response.body(),
                    requestTrace);
        }
        return new HttpJsonResponse(objectMapper.readTree(response.body()), requestTrace);
    }

    private static String responseText(JsonNode response) {
        List<String> parts = new ArrayList<>();
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode content : output.path("content")) {
                String text = content.path("text").asText();
                if (!text.isBlank()) {
                    parts.add(text);
                }
            }
        }
        return String.join("\n", parts);
    }

    private static TokenUsage tokenUsage(JsonNode response) {
        JsonNode usage = response.path("usage");
        return new TokenUsage(
                longValue(usage, "input_tokens"),
                longValue(usage, "output_tokens"),
                longValue(usage, "total_tokens")
        );
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    static boolean supportsReasoning(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalized = model.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4")
                || normalized.startsWith("gpt-5");
    }

    static boolean sendsSamplingParameters(LlmRequestConfig config) {
        return !(config.sendReasoning() && supportsReasoning(config.model()) && config.reasoning() != null
                && config.reasoning() != Reasoning.OFF);
    }

    private static String reasoningValue(LlmRequestConfig config) {
        if (config.reasoningProviderValue() != null && !config.reasoningProviderValue().isBlank()) {
            return config.reasoningProviderValue();
        }
        return config.reasoning().openAiReasoningEffort();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record HttpJsonResponse(JsonNode body, RequestTrace requestTrace) {
    }
}
