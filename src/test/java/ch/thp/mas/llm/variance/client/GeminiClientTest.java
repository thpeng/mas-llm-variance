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

class GeminiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final List<String> paths = new ArrayList<>();
    private final List<String> queries = new ArrayList<>();
    private final List<JsonNode> requests = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsGenerateContentRequestAndResponse() throws Exception {
        startServer(200, """
                {
                  "candidates": [{
                    "content": {
                      "parts": [
                        {"text": "Antwort eins"},
                        {"text": "Antwort zwei"}
                      ]
                    }
                  }],
                  "usageMetadata": {
                    "promptTokenCount": 10,
                    "candidatesTokenCount": 5,
                    "totalTokenCount": 15
                  }
                }
                """);
        GeminiClient client = new GeminiClient("key-1", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        LlmResponse response = client.call("prompt", new LlmRequestConfig(
                "gemini-3-flash",
                0.0,
                1.0,
                1,
                123L,
                Reasoning.LOW
        ));

        assertThat(response.text()).isEqualTo("Antwort eins\nAntwort zwei");
        assertThat(response.tokenUsage()).isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(paths).containsExactly("/models/gemini-3-flash:generateContent");
        assertThat(queries).containsExactly("key=key-1");
        JsonNode request = requests.getFirst();
        assertThat(request.path("contents").get(0).path("parts").get(0).path("text").asText()).isEqualTo("prompt");
        JsonNode generationConfig = request.path("generationConfig");
        assertThat(generationConfig.path("temperature").asDouble()).isEqualTo(0.0);
        assertThat(generationConfig.path("topP").asDouble()).isEqualTo(1.0);
        assertThat(generationConfig.path("topK").asInt()).isEqualTo(1);
        assertThat(generationConfig.path("seed").asInt()).isEqualTo(123);
        assertThat(generationConfig.path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("low");
    }

    @Test
    void mapsOffReasoningToMinimalAndOmitsRandomSeed() throws Exception {
        startServer(200, """
                {
                  "candidates": [{"content": {"parts": [{"text": "Antwort"}]}}],
                  "usageMetadata": {}
                }
                """);
        GeminiClient client = new GeminiClient("key-1", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        client.call("prompt", new LlmRequestConfig("gemini-3-flash", null, null, null, null, Reasoning.OFF));

        JsonNode generationConfig = requests.getFirst().path("generationConfig");
        assertThat(generationConfig.has("seed")).isFalse();
        assertThat(generationConfig.path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("minimal");
    }

    @Test
    void rejectsXhighBeforeRequest() {
        GeminiClient client = new GeminiClient("key-1", "http://localhost:1", HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gemini-3-flash", null, null, null, null, Reasoning.XHIGH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported for Gemini");
    }

    @Test
    void propagatesProviderErrorWithModelContext() throws Exception {
        startServer(400, """
                {"error": {"message": "thinking level not supported"}}
                """);
        GeminiClient client = new GeminiClient("key-1", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gemini-3.1-pro", null, null, null, null, Reasoning.OFF)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gemini-3.1-pro")
                .hasMessageContaining("thinking level not supported");
    }

    @Test
    void failsOnBlankResponseText() throws Exception {
        startServer(200, """
                {"candidates": [{"content": {"parts": [{"text": ""}]}}]}
                """);
        GeminiClient client = new GeminiClient("key-1", baseUrl(), HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.call("prompt", new LlmRequestConfig(
                "gemini-3-flash", null, null, null, null, Reasoning.LOW)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not contain text output");
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            queries.add(exchange.getRequestURI().getQuery());
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!requestBody.isBlank()) {
                requests.add(objectMapper.readTree(requestBody));
            }
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
