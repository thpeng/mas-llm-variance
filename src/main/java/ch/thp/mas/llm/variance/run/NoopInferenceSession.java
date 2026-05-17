package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LlmClient;

class NoopInferenceSession implements InferenceSession {

    private final LlmClient client;

    NoopInferenceSession(LlmClient client) {
        this.client = client;
    }

    @Override
    public LlmClient client() {
        return client;
    }

    @Override
    public ModelInstanceLog modelInstance() {
        return null;
    }

    @Override
    public void close() {
        // No provider lifecycle is needed.
    }
}
