package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;

public record ResolvedPlan(
        String name,
        InferenceProvider inferenceProvider,
        String model,
        String prompt,
        int iterations,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        Reasoning reasoning,
        LmStudioLoadConfig load,
        String modelVersion
) {

    public InferenceProvider getInferenceProvider() {
        return inferenceProvider;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Integer getTopK() {
        return topK;
    }

    public Long getSeed() {
        return seed;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    public LmStudioLoadConfig getLoad() {
        return load;
    }

    public int getIterations() {
        return iterations;
    }
}
