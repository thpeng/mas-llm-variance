package ch.thp.mas.llm.variance.client;

public interface LlmClientFactory {

    LlmClient create(InferenceProvider inferenceProvider);
}
