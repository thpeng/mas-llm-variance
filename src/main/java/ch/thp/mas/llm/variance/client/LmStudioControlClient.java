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
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LmStudioControlClient {

    private final String baseUrl;
    private final String apiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration preflightTimeout = Duration.ofSeconds(15);
    private final Duration lifecycleTimeout = Duration.ofMinutes(10);

    public LmStudioControlClient() {
        this(
                getenv("LMSTUDIO_BASE_URL", "http://127.0.0.1:10022"),
                System.getenv("LM_API_TOKEN"),
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .version(HttpClient.Version.HTTP_1_1)
                        .build(),
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
        if (plan.seedSetting() != null || plan.seed() != null) {
            throw new IllegalArgumentException("LM Studio does not support seed configuration.");
        }
        System.out.println("Checking LM Studio models at " + baseUrl + "/api/v1/models");
        JsonNode models = get("/api/v1/models");
        JsonNode model = findModel(models, plan.model());
        String existingInstanceId = firstLoadedInstanceId(model);
        LmStudioLoadConfigLog loadConfig = loadConfigLog(plan);
        if (existingInstanceId != null) {
            System.out.println("Reusing loaded LM Studio model instance: " + existingInstanceId);
            return new ModelInstanceLog(existingInstanceId, false, loadConfig, model, null, null);
        }

        System.out.println("Loading LM Studio model: " + plan.model());
        JsonNode loadResponse = post("/api/v1/models/load", loadRequest(plan));
        String instanceId = firstText(loadResponse, "instance_id", "id", "model_instance_id");
        System.out.println("Resolving loaded LM Studio model info after load.");
        JsonNode loadedModel = findModel(get("/api/v1/models"), plan.model());
        if (instanceId == null) {
            instanceId = firstLoadedInstanceId(loadedModel);
        }
        if (instanceId == null) {
            throw new IllegalStateException("LM Studio did not report a loaded instance for model: " + plan.model());
        }
        System.out.println("Loaded LM Studio model instance: " + instanceId);
        return new ModelInstanceLog(instanceId, true, loadConfig, loadedModel, loadResponse, null);
    }

    public JsonNode unload(String instanceId) throws Exception {
        System.out.println("Unloading LM Studio model instance: " + instanceId);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("instance_id", instanceId);
        return post("/api/v1/models/unload", request);
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
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(preflightTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .GET();
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
                .timeout(lifecycleTimeout)
                .version(HttpClient.Version.HTTP_1_1)
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

    private static LmStudioLoadConfigLog loadConfigLog(ResolvedPlan plan) {
        LmStudioLoadConfig load = plan.getLoad();
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
