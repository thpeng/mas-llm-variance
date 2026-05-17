package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.LlmClientFactory;
import ch.thp.mas.llm.variance.client.LmStudioControlClient;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import org.springframework.stereotype.Component;

@Component
public class DefaultInferenceSessionFactory implements InferenceSessionFactory {

    private final LlmClientFactory clientFactory;
    private final LmStudioControlClient lmStudioControlClient;

    public DefaultInferenceSessionFactory(
            LlmClientFactory clientFactory,
            LmStudioControlClient lmStudioControlClient
    ) {
        this.clientFactory = clientFactory;
        this.lmStudioControlClient = lmStudioControlClient;
    }

    @Override
    public InferenceSession open(ResolvedPlan plan) throws Exception {
        if (plan.inferenceProvider() == InferenceProvider.LMSTUDIO) {
            return LmStudioInferenceSession.open(plan, lmStudioControlClient);
        }
        return new NoopInferenceSession(clientFactory.create(plan.inferenceProvider()));
    }
}
