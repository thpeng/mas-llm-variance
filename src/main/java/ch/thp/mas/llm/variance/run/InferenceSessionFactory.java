package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.plan.ResolvedPlan;

public interface InferenceSessionFactory {

    InferenceSession open(ResolvedPlan plan) throws Exception;
}
