package ch.thp.mas.llm.variance.run;

import com.fasterxml.jackson.databind.JsonNode;

public record ModelInstanceLog(
        String id,
        boolean loadedByRun,
        LmStudioLoadConfigLog loadConfig,
        JsonNode modelInfoResponse,
        JsonNode loadResponse,
        JsonNode unloadResponse
) {

    public ModelInstanceLog(
            String id,
            boolean loadedByRun,
            LmStudioLoadConfigLog loadConfig,
            JsonNode loadResponse
    ) {
        this(id, loadedByRun, loadConfig, null, loadResponse, null);
    }

    public ModelInstanceLog(
            String id,
            boolean loadedByRun,
            LmStudioLoadConfigLog loadConfig,
            JsonNode loadResponse,
            JsonNode unloadResponse
    ) {
        this(id, loadedByRun, loadConfig, null, loadResponse, unloadResponse);
    }

    public ModelInstanceLog withUnloadResponse(JsonNode unloadResponse) {
        return new ModelInstanceLog(id, loadedByRun, loadConfig, modelInfoResponse, loadResponse, unloadResponse);
    }
}
