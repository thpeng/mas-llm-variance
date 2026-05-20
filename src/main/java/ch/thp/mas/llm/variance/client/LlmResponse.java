package ch.thp.mas.llm.variance.client;

public record LlmResponse(String text, TokenUsage tokenUsage, String modelInstanceId, RequestTrace requestTrace) {

    public LlmResponse(String text, TokenUsage tokenUsage) {
        this(text, tokenUsage, null, null);
    }

    public LlmResponse(String text, TokenUsage tokenUsage, String modelInstanceId) {
        this(text, tokenUsage, modelInstanceId, null);
    }
}
