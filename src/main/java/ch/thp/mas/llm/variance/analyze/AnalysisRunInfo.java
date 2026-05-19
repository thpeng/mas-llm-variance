package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;

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
        String seedSetting,
        Reasoning reasoning,
        boolean sendReasoning,
        String reasoningProviderValue
) {

    public AnalysisRunInfo(
            String planName,
            InferenceProvider inferenceProvider,
            String model,
            String modelVersion,
            int iterations,
            Double temperature,
            Double topP,
            Integer topK,
            Long seed,
            Reasoning reasoning
    ) {
        this(planName, inferenceProvider, model, modelVersion, iterations, temperature, topP, topK, seed,
                seed == null ? "RANDOM" : seed.toString(), reasoning, true, null);
    }
}
