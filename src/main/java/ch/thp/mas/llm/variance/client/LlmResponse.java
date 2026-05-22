package ch.thp.mas.llm.variance.client;

public record LlmResponse(
        String text,
        TokenUsage tokenUsage,
        String modelInstanceId,
        RequestTrace requestTrace,
        String modelVersion
) {

    public LlmResponse(String text, TokenUsage tokenUsage) {
        this(text, tokenUsage, null, null);
    }

    public LlmResponse(String text, TokenUsage tokenUsage, String modelInstanceId) {
        this(text, tokenUsage, modelInstanceId, null);
    }

    public LlmResponse(String text, TokenUsage tokenUsage, String modelInstanceId, RequestTrace requestTrace) {
        this(text, tokenUsage, modelInstanceId, requestTrace, null);
    }
}
