package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.time.OffsetDateTime;
import java.util.List;

public record RunLog(
        String planName,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        InferenceProvider inferenceProvider,
        String model,
        String modelVersion,
        ModelInstanceLog modelInstance,
        int iterations,
        RunConfigLog config,
        String prompt,
        List<RunLogEntry> repetitions
) {
}
