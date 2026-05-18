package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;

public class YamlPlan implements Plan {

    private InferenceProvider inferenceProvider = InferenceProvider.OPENAI;
    private String model;
    private String description;
    private YamlRunConfig run;
    private YamlAnalysisConfig analysis;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public YamlRunConfig getRun() {
        return run;
    }

    public void setRun(YamlRunConfig run) {
        this.run = run;
    }

    public YamlAnalysisConfig getAnalysis() {
        return analysis;
    }

    public void setAnalysis(YamlAnalysisConfig analysis) {
        this.analysis = analysis;
    }

    @Override
    public String getPrompt() {
        return run == null ? prompt : run.getPrompt();
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public Double getTemperature() {
        return run == null ? temperature : run.getTemperature();
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Double getTopP() {
        return run == null ? topP : run.getTopP();
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    @Override
    public Integer getTopK() {
        return run == null ? topK : run.getTopK();
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    @Override
    public Long getSeed() {
        if (run == null) {
            return seed;
        }
        return seedFromRun(run.getSeed());
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    @Override
    public String getReasoning() {
        return run == null ? reasoning : run.getReasoning();
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    @Override
    public LmStudioLoadConfig getLoad() {
        return run == null ? load : run.getLoad();
    }

    public void setLoad(LmStudioLoadConfig load) {
        this.load = load;
    }

    @Override
    public int getIterations() {
        return run == null || run.getIterations() == null ? iterations : run.getIterations();
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    private static Long seedFromRun(String value) {
        if (value == null || value.isBlank() || "random".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return Long.parseLong(value);
    }
}
