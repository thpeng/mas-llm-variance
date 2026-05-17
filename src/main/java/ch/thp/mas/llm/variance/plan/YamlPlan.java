package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;

public class YamlPlan implements Plan {

    private InferenceProvider inferenceProvider = InferenceProvider.OPENAI;
    private String model;
    private String prompt;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Long seed;
    private String reasoning;
    private LmStudioLoadConfig load;
    private int iterations = 30;

    @Override
    public InferenceProvider getInferenceProvider() {
        return inferenceProvider;
    }

    public void setInferenceProvider(InferenceProvider inferenceProvider) {
        this.inferenceProvider = inferenceProvider;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    @Override
    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    @Override
    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    @Override
    public LmStudioLoadConfig getLoad() {
        return load;
    }

    public void setLoad(LmStudioLoadConfig load) {
        this.load = load;
    }

    @Override
    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
}
