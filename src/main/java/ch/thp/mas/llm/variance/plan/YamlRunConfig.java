package ch.thp.mas.llm.variance.plan;

public class YamlRunConfig {

    private String prompt;
    private Integer iterations;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private String seed;
    private String reasoning;
    private Boolean sendReasoning;
    private String reasoningProviderValue;
    private LmStudioLoadConfig load;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getIterations() {
        return iterations;
    }

    public void setIterations(Integer iterations) {
        this.iterations = iterations;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Boolean getSendReasoning() {
        return sendReasoning;
    }

    public void setSendReasoning(Boolean sendReasoning) {
        this.sendReasoning = sendReasoning;
    }

    public String getReasoningProviderValue() {
        return reasoningProviderValue;
    }

    public void setReasoningProviderValue(String reasoningProviderValue) {
        this.reasoningProviderValue = reasoningProviderValue;
    }

    public LmStudioLoadConfig getLoad() {
        return load;
    }

    public void setLoad(LmStudioLoadConfig load) {
        this.load = load;
    }
}
