package ch.thp.mas.llm.variance.client;

public record LlmResponse(String text, TokenUsage tokenUsage, String modelInstanceId) {

    public LlmResponse(String text, TokenUsage tokenUsage) {
        this(text, tokenUsage, null);
    }
}
