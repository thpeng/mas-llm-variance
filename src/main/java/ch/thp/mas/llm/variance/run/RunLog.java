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
        List<RunLogEntry> repetitions,
        RunErrorSummary errors
) {

    public RunLog(
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
        this(planName, startedAt, endedAt, inferenceProvider, model, modelVersion, modelInstance, iterations, config,
                prompt, repetitions, RunErrorSummary.from(repetitions));
    }

    public RunLog {
        repetitions = List.copyOf(repetitions);
        errors = errors == null ? RunErrorSummary.from(repetitions) : errors;
    }
}
