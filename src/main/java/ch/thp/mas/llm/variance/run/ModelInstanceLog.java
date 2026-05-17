package ch.thp.mas.llm.variance.run;

import com.fasterxml.jackson.databind.JsonNode;

public record ModelInstanceLog(
        String id,
        boolean loadedByRun,
        LmStudioLoadConfigLog loadConfig,
        JsonNode loadResponse
) {
}
