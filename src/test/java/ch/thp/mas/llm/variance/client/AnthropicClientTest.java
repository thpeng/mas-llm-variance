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

class AnthropicClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> apiKeyHeaders = new ArrayList<>();
    private final List<String> versionHeaders = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsMessagesRequestAndMapsResponse() throws Exception {
        startServer(200, """
                {
                  "content": [
                    {"type": "text", "text": "Antwort eins"},
                    {"type": "text", "text": "Antwort zwei"}
                  ],
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 5
                  }
                }
                """);
        AnthropicClient client = new AnthropicClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        LlmResponse response = client.call("prompt", new LlmRequestConfig(
                "claude-sonnet-4-6",
                0.0,
                null,
                1,
                null,
                Reasoning.OFF
        ));

        assertThat(response.text()).isEqualTo("Antwort eins\nAntwort zwei");
        assertThat(response.tokenUsage()).isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(response.requestTrace().url()).isEqualTo(baseUrl() + "/messages");
        assertThat(response.requestTrace().headers())
                .containsEntry("Content-Type", List.of("application/json"))
                .containsEntry("anthropic-version", List.of("2023-06-01"));
        assertThat(response.requestTrace().headers()).doesNotContainKey("x-api-key");
        assertThat(apiKeyHeaders).containsExactly("token");
        assertThat(versionHeaders).containsExactly("2023-06-01");
        JsonNode request = requests.getFirst();
        assertThat(request.path("model").asText()).isEqualTo("claude-sonnet-4-6");
        assertThat(request.path("max_tokens").asInt()).isEqualTo(1024);
        assertThat(request.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(request.path("messages").get(0).path("content").asText()).isEqualTo("prompt");
        assertThat(request.path("temperature").asDouble()).isEqualTo(0.0);
        assertThat(request.path("top_k").asInt()).isEqualTo(1);
        assertThat(request.path("thinking").path("type").asText()).isEqualTo("disabled");
        assertThat(request.has("top_p")).isFalse();
        assertThat(request.has("seed")).isFalse();
        assertThat(request.has("output_config")).isFalse();
    }

    @Test
    void sendsAdaptiveThinkingWithEffortAndLargerTokenLimit() throws Exception {
        startServer(200, """
                {
                  "content": [
                    {"type": "thinking", "thinking": "summary"},
                    {"type": "text", "text": "Antwort"}
                  ],
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 5
                  }
                }
                """);
        AnthropicClient client = new AnthropicClient("token", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        LlmResponse response = client.call("prompt", new LlmRequestConfig(
                "claude-sonnet-4-6",
                null,
                1.0,
                null,
                null,
                Reasoning.MEDIUM
        ));

        assertThat(response.text()).isEqualTo("Antwort");
        JsonNode request = requests.getFirst();
        assertThat(request.path("max_tokens").asInt()).isEqualTo(4096);
        assertThat(request.path("thinking").path("type").asText()).isEqualTo("adaptive");
        assertThat(request.path("output_config").path("effort").asText()).isEqualTo("medium");
    }

    @Test
    void mapsAnthropicEffortValues() {
        assertThat(Reasoning.LOW.anthropicEffort()).isEqualTo("low");
        assertThat(Reasoning.MEDIUM.anthropicEffort()).isEqualTo("medium");
        assertThat(Reasoning.HIGH.anthropicEffort()).isEqualTo("high");
        assertThatThrownBy(Reasoning.OFF::anthropicEffort)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disables Anthropic thinking");
        assertThatThrownBy(Reasoning.XHIGH::anthropicEffort)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anthropic");
    }

    @Test
    void usesTemperatureWhenItIsNonZero() {
        LlmRequestConfig config = new LlmRequestConfig("claude", 0.2, null, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isTrue();
        assertThat(AnthropicClient.useTopP(config)).isFalse();
    }

    @Test
    void usesTemperatureWhenItIsZero() {
        LlmRequestConfig config = new LlmRequestConfig("claude", 0.0, null, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isTrue();
        assertThat(AnthropicClient.useTopP(config)).isFalse();
    }

    @Test
    void usesTopPWhenTemperatureIsUnset() {
        LlmRequestConfig config = new LlmRequestConfig("claude", null, 0.9, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isFalse();
        assertThat(AnthropicClient.useTopP(config)).isTrue();
    }

    @Test
    void omitsBothWhenSamplingIsUnset() {
        LlmRequestConfig config = new LlmRequestConfig("claude", null, null, null, null, Reasoning.OFF);

        assertThat(AnthropicClient.useTemperature(config)).isFalse();
        assertThat(AnthropicClient.useTopP(config)).isFalse();
    }

    @Test
    void rejectsUnsupportedSeedCombinedSamplingAndThinkingTopK() {
        AnthropicClient client = new AnthropicClient("token", "http://localhost:1", HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "claude-sonnet-4-6", null, null, null, 1L, Reasoning.OFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed");
        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "claude-sonnet-4-6", 0.2, 1.0, null, null, Reasoning.OFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temperature and topP");
        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "claude-sonnet-4-6", null, null, 1, null, Reasoning.LOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK when thinking is enabled");
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/messages", exchange -> {
            apiKeyHeaders.add(exchange.getRequestHeaders().getFirst("x-api-key"));
            versionHeaders.add(exchange.getRequestHeaders().getFirst("anthropic-version"));
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
