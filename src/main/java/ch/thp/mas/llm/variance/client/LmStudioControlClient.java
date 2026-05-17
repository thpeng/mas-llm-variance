package ch.thp.mas.llm.variance.client;

import ch.thp.mas.llm.variance.plan.LmStudioLoadConfig;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.LmStudioLoadConfigLog;
import ch.thp.mas.llm.variance.run.ModelInstanceLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class LmStudioControlClient {

    private final String baseUrl;
    private final String apiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LmStudioControlClient() {
        this(
                getenv("LMSTUDIO_BASE_URL", "http://localhost:1234"),
                System.getenv("LM_API_TOKEN"),
                HttpClient.newHttpClient(),
                new ObjectMapper()
        );
    }

    LmStudioControlClient(String baseUrl, String apiToken, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiToken = apiToken;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ModelInstanceLog ensureLoaded(ResolvedPlan plan) throws Exception {
        JsonNode models = get("/api/v1/models");
        JsonNode model = findModel(models, plan.model());
        String existingInstanceId = firstLoadedInstanceId(model);
        LmStudioLoadConfigLog loadConfig = loadConfigLog(plan.getLoad());
        if (existingInstanceId != null) {
            return new ModelInstanceLog(existingInstanceId, false, loadConfig, null);
        }

        JsonNode loadResponse = post("/api/v1/models/load", loadRequest(plan));
        String instanceId = firstText(loadResponse, "instance_id", "id", "model_instance_id");
        if (instanceId == null) {
            JsonNode reloadedModels = get("/api/v1/models");
            instanceId = firstLoadedInstanceId(findModel(reloadedModels, plan.model()));
        }
        if (instanceId == null) {
            throw new IllegalStateException("LM Studio did not report a loaded instance for model: " + plan.model());
        }
        return new ModelInstanceLog(instanceId, true, loadConfig, loadResponse);
    }

    public void unload(String instanceId) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("instance_id", instanceId);
        post("/api/v1/models/unload", request);
    }

    private ObjectNode loadRequest(ResolvedPlan plan) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", plan.model());
        request.put("echo_load_config", true);
        LmStudioLoadConfig load = plan.getLoad();
        if (load != null) {
            putIfNotNull(request, "context_length", load.getContextLength());
            putIfNotNull(request, "eval_batch_size", load.getEvalBatchSize());
            putIfNotNull(request, "flash_attention", load.getFlashAttention());
            putIfNotNull(request, "num_experts", load.getNumExperts());
            putIfNotNull(request, "offload_kv_cache_to_gpu", load.getOffloadKvCacheToGpu());
        }
        return request;
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        addAuthorization(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(path + " failed with HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
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

    private JsonNode findModel(JsonNode modelsResponse, String modelKey) {
        for (JsonNode model : modelsResponse.path("models")) {
            if (modelKey.equals(model.path("key").asText())) {
                return model;
            }
        }
        throw new IllegalStateException("LM Studio model is not available: " + modelKey);
    }

    private static String firstLoadedInstanceId(JsonNode model) {
        JsonNode loadedInstances = model.path("loaded_instances");
        if (!loadedInstances.isArray() || loadedInstances.isEmpty()) {
            return null;
        }
        JsonNode id = loadedInstances.get(0).path("id");
        return id.isTextual() ? id.asText() : null;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual()) {
                return value.asText();
            }
        }
        return null;
    }

    private static LmStudioLoadConfigLog loadConfigLog(LmStudioLoadConfig load) {
        if (load == null) {
            return new LmStudioLoadConfigLog(null, null, null, null, null);
        }
        return new LmStudioLoadConfigLog(
                load.getContextLength(),
                load.getEvalBatchSize(),
                load.getFlashAttention(),
                load.getNumExperts(),
                load.getOffloadKvCacheToGpu()
        );
    }

    private static void putIfNotNull(ObjectNode node, String field, Integer value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    private static void putIfNotNull(ObjectNode node, String field, Boolean value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
