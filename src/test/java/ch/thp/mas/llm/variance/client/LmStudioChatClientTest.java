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

class LmStudioChatClientTest {

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
    void sendsNativeChatRequestAndMapsMessageAndTokenUsage() throws Exception {
        startServer(200, """
                {
                  "model_instance_id": "instance-1",
                  "output": [
                    {"type": "reasoning", "content": "hidden"},
                    {"type": "message", "content": "Antwort eins"},
                    {"type": "tool_call", "tool": "ignored", "arguments": {}, "output": "ignored"},
                    {"type": "message", "content": "Antwort zwei"}
                  ],
                  "stats": {
                    "input_tokens": 10,
                    "total_output_tokens": 5,
                    "reasoning_output_tokens": 2
                  }
                }
                """);
        LmStudioChatClient client = new LmStudioChatClient(baseUrl(), "token", HttpClient.newHttpClient(), objectMapper);

        LlmResponse response = client.call("prompt", new LlmRequestConfig(
                "model-a",
                0.0,
                1.0,
                1,
                null,
                Reasoning.HIGH
        ));

        assertThat(response.text()).isEqualTo("Antwort eins\nAntwort zwei");
        assertThat(response.tokenUsage()).isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(response.modelInstanceId()).isEqualTo("instance-1");
        assertThat(response.requestTrace().url()).isEqualTo(baseUrl() + "/api/v1/chat");
        assertThat(response.requestTrace().headers())
                .containsEntry("Content-Type", List.of("application/json"));
        assertThat(response.requestTrace().headers()).doesNotContainKey("Authorization");
        assertThat(requests).hasSize(1);
        JsonNode request = requests.getFirst();
        assertThat(request.path("model").asText()).isEqualTo("model-a");
        assertThat(request.path("input").asText()).isEqualTo("prompt");
        assertThat(request.path("temperature").asDouble()).isEqualTo(0.0);
        assertThat(request.path("top_p").asDouble()).isEqualTo(1.0);
        assertThat(request.path("top_k").asInt()).isEqualTo(1);
        assertThat(request.path("reasoning").asText()).isEqualTo("high");
        assertThat(request.path("store").asBoolean()).isFalse();
        assertThat(authorizationHeaders).containsExactly("Bearer token");
    }

    @Test
    void omitsOffReasoningAndNullGenerationParameters() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": "Antwort"}],
                  "stats": {}
                }
                """);
        LmStudioChatClient client = new LmStudioChatClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig("model-a", null, null, null, null, Reasoning.OFF, false));

        JsonNode request = requests.getFirst();
        assertThat(request.has("reasoning")).isFalse();
        assertThat(request.has("temperature")).isFalse();
        assertThat(request.has("top_p")).isFalse();
        assertThat(request.has("top_k")).isFalse();
        assertThat(authorizationHeaders).containsExactly("<none>");
    }

    @Test
    void sendsOffReasoningWhenConfiguredExplicitly() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": "Antwort"}],
                  "stats": {}
                }
                """);
        LmStudioChatClient client = new LmStudioChatClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig("model-a", null, null, null, null, Reasoning.OFF, true));

        assertThat(requests.getFirst().path("reasoning").asText()).isEqualTo("off");
    }

    @Test
    void sendsProviderReasoningValueWhenConfigured() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "message", "content": "Antwort"}],
                  "stats": {}
                }
                """);
        LmStudioChatClient client = new LmStudioChatClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig("model-a", null, null, null, null, Reasoning.HIGH, true, "on"));

        assertThat(requests.getFirst().path("reasoning").asText()).isEqualTo("on");
    }

    @Test
    void rejectsXhighReasoningForLmStudio() {
        LmStudioChatClient client = new LmStudioChatClient("http://localhost:1", null, HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig("model-a", null, null, null, null, Reasoning.XHIGH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported for LM Studio");
    }

    @Test
    void failsWhenResponseHasNoMessageOutput() throws Exception {
        startServer(200, """
                {
                  "output": [{"type": "reasoning", "content": "nur reasoning"}],
                  "stats": {"input_tokens": 1, "total_output_tokens": 2}
                }
                """);
        LmStudioChatClient client = new LmStudioChatClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig("model-a", null, null, null, null, Reasoning.LOW)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not contain a message output");
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/chat", exchange -> {
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization") == null
                    ? "<none>"
                    : exchange.getRequestHeaders().getFirst("Authorization"));
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
