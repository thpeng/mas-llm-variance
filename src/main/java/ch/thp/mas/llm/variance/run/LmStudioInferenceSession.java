package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LmStudioChatClient;
import ch.thp.mas.llm.variance.client.LmStudioControlClient;
import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;

class LmStudioInferenceSession implements InferenceSession {

    private final LmStudioControlClient controlClient;
    private final LlmClient client;
    private final ModelInstanceLog modelInstance;

    private LmStudioInferenceSession(
            LmStudioControlClient controlClient,
            LlmClient client,
            ModelInstanceLog modelInstance
    ) {
        this.controlClient = controlClient;
        this.client = client;
        this.modelInstance = modelInstance;
    }

    static LmStudioInferenceSession open(ResolvedPlan plan, LmStudioControlClient controlClient) throws Exception {
        ModelInstanceLog modelInstance = controlClient.ensureLoaded(plan);
        String baseUrl = getenv("LMSTUDIO_BASE_URL", "http://127.0.0.1:10022");
        LlmClient client = new LmStudioChatClient(baseUrl, System.getenv("LM_API_TOKEN"));
        return new LmStudioInferenceSession(controlClient, client, modelInstance);
    }

    @Override
    public LlmClient client() {
        return client;
    }

    @Override
    public ModelInstanceLog modelInstance() {
        return modelInstance;
    }

    @Override
    public void close() throws Exception {
        if (modelInstance.loadedByRun()) {
            controlClient.unload(modelInstance.id());
        }
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
