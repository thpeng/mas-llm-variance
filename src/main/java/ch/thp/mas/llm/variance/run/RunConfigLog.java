package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.Reasoning;

public record RunConfigLog(
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        String seedSetting,
        Reasoning reasoning,
        boolean sendReasoning,
        String reasoningProviderValue
) {

    public RunConfigLog(
            Double temperature,
            Double topP,
            Integer topK,
            Long seed,
            Reasoning reasoning
    ) {
        this(temperature, topP, topK, seed, seed == null ? "RANDOM" : seed.toString(), reasoning, true, null);
    }
}
