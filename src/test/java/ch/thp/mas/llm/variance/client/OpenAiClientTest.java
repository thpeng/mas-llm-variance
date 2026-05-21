package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> authorizationHeaders = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsResponsesRequestAndMapsResponse() throws Exception {
        startServer(200, """
                {
                  "output": [{
                    "type": "message",
                    "content": [
                      {"type": "output_text", "text": "Antwort eins"},
                      {"type": "output_text", "text": "Antwort zwei"}
                    ]
                  }],
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 5,
                    "total_tokens": 15
                  }
                }
                """);
        OpenAiClient client = new OpenAiClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        LlmResponse response = client.call("prompt", new LlmRequestConfig(
                "gpt-4o-2024-08-06",
                0.0,
                1.0,
                null,
                null,
                Reasoning.OFF,
                false
        ));

        assertThat(response.text()).isEqualTo("Antwort eins\nAntwort zwei");
        assertThat(response.tokenUsage()).isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(response.requestTrace().url()).isEqualTo(baseUrl() + "/responses");
        assertThat(response.requestTrace().headers())
                .containsEntry("Content-Type", List.of("application/json"));
        assertThat(response.requestTrace().headers()).doesNotContainKey("Authorization");
        assertThat(authorizationHeaders).containsExactly("Bearer token");
        JsonNode request = requests.getFirst();
        assertThat(request.path("model").asText()).isEqualTo("gpt-4o-2024-08-06");
        assertThat(request.path("input").asText()).isEqualTo("prompt");
        assertThat(request.path("temperature").asDouble()).isEqualTo(0.0);
        assertThat(request.path("top_p").asDouble()).isEqualTo(1.0);
        assertThat(request.has("top_k")).isFalse();
        assertThat(request.has("seed")).isFalse();
        assertThat(request.has("reasoning")).isFalse();
    }

    @Test
    void sendsReasoningForReasoningModelAndOmitsSamplingParameters() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": [{"type": "output_text", "text": "Antwort"}]}],
                  "usage": {}
                }
                """);
        OpenAiClient client = new OpenAiClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig(
                "gpt-5.4-mini-2026-03-17",
                null,
                null,
                null,
                null,
                Reasoning.HIGH
        ));

        JsonNode request = requests.getFirst();
        assertThat(request.path("reasoning").path("effort").asText()).isEqualTo("high");
        assertThat(request.has("temperature")).isFalse();
        assertThat(request.has("top_p")).isFalse();
    }

    @Test
    void rejectsConfiguredSamplingForReasoningRequest() {
        OpenAiClient client = new OpenAiClient("token", "http://localhost:1", HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gpt-5.4-mini-2026-03-17",
                0.7,
                null,
                null,
                null,
                Reasoning.HIGH
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampling");
    }

    @Test
    void rejectsUnsupportedTopKAndSeed() {
        OpenAiClient client = new OpenAiClient("token", "http://localhost:1", HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gpt-4o-2024-08-06", null, null, 1, null, Reasoning.OFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gpt-4o-2024-08-06", null, null, null, 1L, Reasoning.OFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed");
    }

    @Test
    void sendsNoneReasoningForReasoningModelAndKeepsSamplingParameters() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": [{"type": "output_text", "text": "Antwort"}]}],
                  "usage": {}
                }
                """);
        OpenAiClient client = new OpenAiClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig(
                "gpt-5.4-mini-2026-03-17",
                0.7,
                1.0,
                null,
                null,
                Reasoning.OFF
        ));

        JsonNode request = requests.getFirst();
        assertThat(request.path("reasoning").path("effort").asText()).isEqualTo("none");
        assertThat(request.path("temperature").asDouble()).isEqualTo(0.7);
        assertThat(request.path("top_p").asDouble()).isEqualTo(1.0);
    }

    @Test
    void omitsNoneReasoningForNonReasoningModel() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": [{"type": "output_text", "text": "Antwort"}]}],
                  "usage": {}
                }
                """);
        OpenAiClient client = new OpenAiClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig(
                "gpt-4o-2024-08-06",
                0.7,
                1.0,
                null,
                null,
                Reasoning.OFF
        ));

        assertThat(requests.getFirst().has("reasoning")).isFalse();
    }


    @Test
    void detectsOpenAiModelsWithReasoningSupport() {
        assertThat(OpenAiClient.supportsReasoning("o1-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("o3-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("o4-mini")).isTrue();
        assertThat(OpenAiClient.supportsReasoning("gpt-5-mini-2025-08-07")).isTrue();
    }

    @Test
    void detectsOpenAiModelsWithoutReasoningSupport() {
        assertThat(OpenAiClient.supportsReasoning("gpt-4o")).isFalse();
        assertThat(OpenAiClient.supportsReasoning("gpt-4o-mini")).isFalse();
        assertThat(OpenAiClient.supportsReasoning("gpt-4.1")).isFalse();
        assertThat(OpenAiClient.supportsReasoning(null)).isFalse();
        assertThat(OpenAiClient.supportsReasoning("")).isFalse();
    }

    @Test
    void omitsSamplingParametersForReasoningModelWhenReasoningIsEnabled() {
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.LOW))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.MEDIUM))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.HIGH))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.XHIGH))).isFalse();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-5.4-mini-2026-03-17", Reasoning.OFF))).isTrue();
        assertThat(OpenAiClient.sendsSamplingParameters(
                new LlmRequestConfig("gpt-5.4-mini-2026-03-17", 0.0, 1.0, null, null, Reasoning.HIGH, false)))
                .isTrue();
        assertThat(OpenAiClient.sendsSamplingParameters(
                config("gpt-4o-2024-11-20", Reasoning.LOW))).isTrue();
    }

    private static LlmRequestConfig config(String model, Reasoning reasoning) {
        return new LlmRequestConfig(model, 0.0, 1.0, null, null, reasoning);
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(objectMapper.readTree(requestBody));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
