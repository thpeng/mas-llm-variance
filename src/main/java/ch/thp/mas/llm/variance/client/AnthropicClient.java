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

public class AnthropicClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build(), new ObjectMapper());
    }

    AnthropicClient(String apiKey, String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
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
        return new LlmResponse(text.trim(), tokenUsage(response), null, httpResponse.requestTrace(),
                textValue(response, "model"));
    }

    private ObjectNode request(String prompt, LlmRequestConfig config) {
        validateConfig(config);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", config.model());
        request.put("max_tokens", useAdaptiveThinking(config) ? 4096 : 1024);
        request.putArray("messages").add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", prompt));

        if (useTemperature(config)) {
            request.put("temperature", config.temperature());
        } else if (useTopP(config)) {
            request.put("top_p", config.topP());
        }
        if (config.topK() != null) {
            request.put("top_k", config.topK());
        }
        if (config.sendReasoning() && config.reasoning() != null) {
            if (config.reasoning() == Reasoning.OFF) {
                request.set("thinking", objectMapper.createObjectNode().put("type", "disabled"));
            } else {
                request.set("thinking", objectMapper.createObjectNode().put("type", "adaptive"));
                request.set("output_config", objectMapper.createObjectNode()
                        .put("effort", config.reasoning().anthropicEffort()));
            }
        }
        return request;
    }

    private HttpJsonResponse post(JsonNode body) throws Exception {
        String requestBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(10))
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        RequestTrace requestTrace = RequestTrace.of(request, requestBody, response);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServingException("Anthropic messages failed with HTTP "
                    + response.statusCode() + ": " + response.body(), response.statusCode(), response.body(),
                    requestTrace);
        }
        return new HttpJsonResponse(objectMapper.readTree(response.body()), requestTrace);
    }

    private static String responseText(JsonNode response) {
        List<String> parts = new ArrayList<>();
        for (JsonNode content : response.path("content")) {
            if (!"text".equals(content.path("type").asText())) {
                continue;
            }
            String text = content.path("text").asText();
            if (!text.isBlank()) {
                parts.add(text);
            }
        }
        return String.join("\n", parts);
    }

    private static TokenUsage tokenUsage(JsonNode response) {
        JsonNode usage = response.path("usage");
        return TokenUsage.of(
                longValue(usage, "input_tokens"),
                longValue(usage, "output_tokens")
        );
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    static boolean useTemperature(LlmRequestConfig config) {
        return config.temperature() != null;
    }

    static boolean useTopP(LlmRequestConfig config) {
        return !useTemperature(config) && config.topP() != null;
    }

    static boolean useAdaptiveThinking(LlmRequestConfig config) {
        return config.sendReasoning() && config.reasoning() != null && config.reasoning() != Reasoning.OFF;
    }

    private static void validateConfig(LlmRequestConfig config) {
        if (config.seed() != null) {
            throw new IllegalArgumentException("Anthropic Messages API does not support seed.");
        }
        if (config.reasoningProviderValue() != null && !config.reasoningProviderValue().isBlank()) {
            throw new IllegalArgumentException("Anthropic reasoningProviderValue is not supported by this client.");
        }
        if (config.temperature() != null && config.topP() != null) {
            throw new IllegalArgumentException("Anthropic Messages API does not allow temperature and topP together.");
        }
        if (useAdaptiveThinking(config) && config.topK() != null) {
            throw new IllegalArgumentException("Anthropic Messages API does not allow topK when thinking is enabled.");
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record HttpJsonResponse(JsonNode body, RequestTrace requestTrace) {
    }
}
