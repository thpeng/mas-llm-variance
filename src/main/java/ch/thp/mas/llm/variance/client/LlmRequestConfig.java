package ch.thp.mas.llm.variance.client;

public record LlmRequestConfig(
        String model,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        String reasoning
) {
}
