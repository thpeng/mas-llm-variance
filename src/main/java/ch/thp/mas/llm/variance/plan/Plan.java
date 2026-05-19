package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;

public interface Plan {

    InferenceProvider getInferenceProvider();

    String getModel();

    String getPrompt();

    Double getTemperature();

    Double getTopP();

    Integer getTopK();

    Long getSeed();

    String getReasoning();

    Boolean getSendReasoning();

    String getReasoningProviderValue();

    LmStudioLoadConfig getLoad();

    int getIterations();
}
