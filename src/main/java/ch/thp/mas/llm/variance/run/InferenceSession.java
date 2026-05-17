package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LlmClient;

public interface InferenceSession extends AutoCloseable {

    LlmClient client();

    ModelInstanceLog modelInstance();

    @Override
    void close() throws Exception;
}
