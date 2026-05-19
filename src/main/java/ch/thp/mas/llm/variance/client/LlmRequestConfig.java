package ch.thp.mas.llm.variance.client;

public record LlmRequestConfig(
        String model,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        Reasoning reasoning,
        boolean sendReasoning,
        String reasoningProviderValue
) {

    public LlmRequestConfig(
            String model,
            Double temperature,
            Double topP,
            Integer topK,
            Long seed,
            Reasoning reasoning
    ) {
        this(model, temperature, topP, topK, seed, reasoning, true, null);
    }

    public LlmRequestConfig(
            String model,
            Double temperature,
            Double topP,
            Integer topK,
            Long seed,
            Reasoning reasoning,
            boolean sendReasoning
    ) {
        this(model, temperature, topP, topK, seed, reasoning, sendReasoning, null);
    }
}
