package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.ModelInstanceLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "LMSTUDIO_INTEGRATION_MODEL", matches = ".+")
class LmStudioControlClientIntegrationTest {

    private static final String BASE_URL = getenv("LMSTUDIO_BASE_URL", "http://127.0.0.1:10022");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final LmStudioControlClient controlClient = new LmStudioControlClient(
            BASE_URL,
            System.getenv("LM_API_TOKEN"),
            httpClient,
            objectMapper
    );

    @Test
    void loadsRecordsModelInfoAndUnloadsModelInstance() throws Exception {
        String model = System.getenv("LMSTUDIO_INTEGRATION_MODEL");
        ModelInstanceLog modelInstance = null;
        try {
            assertModelIsNotLoaded(model);

            modelInstance = controlClient.ensureLoaded(plan(model));

            assertThat(modelInstance.id()).isNotBlank();
            assertThat(modelInstance.loadedByRun()).isTrue();
            assertThat(modelInstance.loadResponse()).isNotNull();
            assertThat(modelInstance.modelInfoResponse()).isNotNull();
            assertThat(modelInstance.modelInfoResponse().path("key").asText()).isEqualTo(model);
            assertThat(modelInstance.modelInfoResponse().path("loaded_instances").isArray()).isTrue();
        } finally {
            if (modelInstance != null && modelInstance.loadedByRun()) {
                controlClient.unload(modelInstance.id());
            }
        }
    }

    private void assertModelIsNotLoaded(String model) throws Exception {
        JsonNode modelInfo = findModel(models(), model);
        JsonNode loadedInstances = modelInfo.path("loaded_instances");
        assertThat(loadedInstances.isMissingNode() || !loadedInstances.isArray() || loadedInstances.isEmpty())
                .as("LM Studio model must not already be loaded before this integration test: %s", model)
                .isTrue();
    }

    private JsonNode models() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(stripTrailingSlash(BASE_URL) + "/api/v1/models"))
                .timeout(Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_1_1)
                .GET();
        String token = System.getenv("LM_API_TOKEN");
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("LM Studio /api/v1/models response body: %s", response.body())
                .isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private static JsonNode findModel(JsonNode modelsResponse, String modelKey) {
        for (JsonNode model : modelsResponse.path("models")) {
            if (modelKey.equals(model.path("key").asText())) {
                return model;
            }
        }
        throw new IllegalStateException("LM Studio model is not available: " + modelKey);
    }

    private static ResolvedPlan plan(String model) {
        return new ResolvedPlan(
                "lmstudio-integration",
                InferenceProvider.LMSTUDIO,
                model,
                "Say ok.",
                1,
                null,
                null,
                null,
                null,
                null,
                Reasoning.OFF,
                false,
                null,
                null,
                null
        );
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
