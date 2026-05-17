package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.client.InferenceProvider;

public record AnalysisRunInfo(
        String planName,
        InferenceProvider inferenceProvider,
        String model,
        String modelVersion,
        int iterations,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        String reasoning
) {
}
