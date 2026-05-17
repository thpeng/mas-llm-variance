package ch.thp.mas.llm.variance.client;

import org.springframework.stereotype.Component;

@Component
public class DefaultLlmClientFactory implements LlmClientFactory {

    @Override
    public LlmClient create(InferenceProvider inferenceProvider) {
        return inferenceProvider.createClient();
    }
}
