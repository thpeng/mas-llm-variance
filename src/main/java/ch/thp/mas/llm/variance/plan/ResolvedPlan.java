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
        String seedSetting,
        Reasoning reasoning,
        boolean sendReasoning,
        String reasoningProviderValue,
        LmStudioLoadConfig load,
        String modelVersion,
        String sourcePath
) {

    public ResolvedPlan(
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
        this(name, inferenceProvider, model, prompt, iterations, temperature, topP, topK, seed,
                seed == null ? null : seed.toString(), reasoning, true, null, load, modelVersion, "");
    }

    public ResolvedPlan(
            String name,
            InferenceProvider inferenceProvider,
            String model,
            String prompt,
            int iterations,
            Double temperature,
            Double topP,
            Integer topK,
            Long seed,
            String seedSetting,
            Reasoning reasoning,
            boolean sendReasoning,
            String reasoningProviderValue,
            LmStudioLoadConfig load,
            String modelVersion
    ) {
        this(name, inferenceProvider, model, prompt, iterations, temperature, topP, topK, seed, seedSetting,
                reasoning, sendReasoning, reasoningProviderValue, load, modelVersion, "");
    }

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

    public String getSeedSetting() {
        return seedSetting;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    public boolean getSendReasoning() {
        return sendReasoning;
    }

    public String getReasoningProviderValue() {
        return reasoningProviderValue;
    }

    public LmStudioLoadConfig getLoad() {
        return load;
    }

    public int getIterations() {
        return iterations;
    }
}
