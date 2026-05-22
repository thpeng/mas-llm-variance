package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.plan.LmStudioLoadConfig;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.ModelInstanceLog;
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

class LmStudioControlClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final List<String> paths = new ArrayList<>();
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> authorizationHeaders = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loadsMissingModelAndUnloadsResolvedInstance() throws Exception {
        startServer("""
                {"models":[{"key":"model-a","loaded_instances":[]}]}
                """, """
                {"status":"loaded","instance_id":"instance-a","load_config":{"context_length":8192}}
                """);
        LmStudioControlClient client = new LmStudioControlClient(baseUrl(), "token", HttpClient.newHttpClient(), objectMapper);

        ModelInstanceLog modelInstance = client.ensureLoaded(plan(loadConfig()));
        JsonNode unloadResponse = client.unload(modelInstance.id());

        assertThat(modelInstance.id()).isEqualTo("instance-a");
        assertThat(modelInstance.loadedByRun()).isTrue();
        assertThat(modelInstance.loadConfig().contextLength()).isEqualTo(8192);
        assertThat(modelInstance.modelInfoResponse().path("key").asText()).isEqualTo("model-a");
        assertThat(modelInstance.loadResponse().path("status").asText()).isEqualTo("loaded");
        assertThat(unloadResponse.path("status").asText()).isEqualTo("unloaded");
        assertThat(paths).containsExactly("/api/v1/models", "/api/v1/models/load", "/api/v1/models", "/api/v1/models/unload");
        assertThat(authorizationHeaders).containsExactly("Bearer token", "Bearer token", "Bearer token", "Bearer token");
        JsonNode loadRequest = requests.getFirst();
        assertThat(loadRequest.path("model").asText()).isEqualTo("model-a");
        assertThat(loadRequest.has("seed")).isFalse();
        assertThat(loadRequest.path("context_length").asInt()).isEqualTo(8192);
        assertThat(loadRequest.path("eval_batch_size").asInt()).isEqualTo(512);
        assertThat(loadRequest.path("flash_attention").asBoolean()).isTrue();
        assertThat(loadRequest.path("offload_kv_cache_to_gpu").asBoolean()).isTrue();
        assertThat(loadRequest.path("echo_load_config").asBoolean()).isTrue();
        assertThat(requests.get(1).path("instance_id").asText()).isEqualTo("instance-a");
    }

    @Test
    void reusesAlreadyLoadedModelWithoutLoadResponse() throws Exception {
        startServer("""
                {"models":[{"key":"model-a","loaded_instances":[{"id":"existing"}]}]}
                """, """
                {"status":"loaded"}
                """);
        LmStudioControlClient client = new LmStudioControlClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        ModelInstanceLog modelInstance = client.ensureLoaded(plan(null, null));

        assertThat(modelInstance.id()).isEqualTo("existing");
        assertThat(modelInstance.loadedByRun()).isFalse();
        assertThat(modelInstance.modelInfoResponse().path("key").asText()).isEqualTo("model-a");
        assertThat(modelInstance.loadResponse()).isNull();
        assertThat(paths).containsExactly("/api/v1/models");
        assertThat(requests).isEmpty();
    }

    @Test
    void rejectsConfiguredSeed() throws Exception {
        startServer("""
                {"models":[{"key":"model-a","loaded_instances":[]}]}
                """, """
                {"status":"loaded"}
                """);
        LmStudioControlClient client = new LmStudioControlClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.ensureLoaded(plan(null, 123L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not support seed");
        assertThat(paths).isEmpty();
    }

    @Test
    void failsWhenModelIsMissing() throws Exception {
        startServer("""
                {"models":[{"key":"other","loaded_instances":[]}]}
                """, """
                {"status":"loaded"}
                """);
        LmStudioControlClient client = new LmStudioControlClient(baseUrl(), null, HttpClient.newHttpClient(), objectMapper);

        assertThatThrownBy(() -> client.ensureLoaded(plan(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("model-a");
    }

    private void startServer(String modelsResponse, String loadResponse) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/models", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            recordAuthorization(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = modelsResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/api/v1/models/load", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            recordAuthorization(exchange.getRequestHeaders().getFirst("Authorization"));
            requests.add(objectMapper.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] response = loadResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/api/v1/models/unload", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            recordAuthorization(exchange.getRequestHeaders().getFirst("Authorization"));
            requests.add(objectMapper.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] response = "{\"status\":\"unloaded\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private void recordAuthorization(String value) {
        authorizationHeaders.add(value == null ? "<none>" : value);
    }

    private ResolvedPlan plan(LmStudioLoadConfig loadConfig) {
        return plan(loadConfig, null);
    }

    private ResolvedPlan plan(LmStudioLoadConfig loadConfig, Long seed) {
        return new ResolvedPlan(
                "0001-test",
                InferenceProvider.LMSTUDIO,
                "model-a",
                "prompt",
                1,
                null,
                null,
                null,
                seed,
                seed == null ? null : seed.toString(),
                Reasoning.OFF,
                true,
                null,
                loadConfig,
                null
        );
    }

    private static LmStudioLoadConfig loadConfig() {
        LmStudioLoadConfig loadConfig = new LmStudioLoadConfig();
        loadConfig.setContextLength(8192);
        loadConfig.setEvalBatchSize(512);
        loadConfig.setFlashAttention(true);
        loadConfig.setOffloadKvCacheToGpu(true);
        return loadConfig;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
