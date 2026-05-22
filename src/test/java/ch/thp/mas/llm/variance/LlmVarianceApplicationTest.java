package ch.thp.mas.llm.variance;

import static org.mockito.Mockito.verify;

import ch.thp.mas.llm.variance.analyze.AnalyzeCommand;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.PlanRunner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(args = {"--run=0000-openai-gpt4o-20240513-roundtrip-de-baseline", "--iterations=1"})
class LlmVarianceApplicationTest {

    @MockitoBean
    private PlanRunner planRunner;

    @MockitoBean
    private AnalyzeCommand analyzeCommand;

    @Test
    void wiresCommandLinePlanToRunner() throws Exception {
        verify(planRunner).run(ArgumentMatchers.argThat((ResolvedPlan plan) ->
                plan.name().equals("0000-openai-gpt4o-20240513-roundtrip-de-baseline") && plan.iterations() == 1
        ), ArgumentMatchers.any());
    }
}
