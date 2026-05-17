package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;

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
        String reasoning,
        LmStudioLoadConfig load,
        String modelVersion
) implements Plan {

    @Override
    public InferenceProvider getInferenceProvider() {
        return inferenceProvider;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    @Override
    public Double getTopP() {
        return topP;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    @Override
    public Long getSeed() {
        return seed;
    }

    @Override
    public String getReasoning() {
        return reasoning;
    }

    @Override
    public LmStudioLoadConfig getLoad() {
        return load;
    }

    @Override
    public int getIterations() {
        return iterations;
    }
}
