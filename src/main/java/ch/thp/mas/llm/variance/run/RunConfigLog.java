package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.Reasoning;

public record RunConfigLog(
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        Reasoning reasoning
) {
}
