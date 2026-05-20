package ch.thp.mas.llm.variance.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseUsage;
import java.util.List;
import java.util.Map;

public class OpenAiClient implements LlmClient {

    private final OpenAIClient client;

    public OpenAiClient(String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
                .model(config.model())
                .input(prompt);

        if (sendsSamplingParameters(config) && config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        if (sendsSamplingParameters(config) && config.topP() != null) {
            builder.topP(config.topP());
        }
        if (config.sendReasoning() && config.reasoning() != null && supportsReasoning(config.model())) {
            builder.reasoning(com.openai.models.Reasoning.builder()
                    .effort(ReasoningEffort.of(reasoningValue(config)))
                    .build());
        } else if (config.sendReasoning() && config.reasoning() != null && config.reasoning() != Reasoning.OFF) {
            throw new IllegalArgumentException("OpenAI model '" + config.model()
                    + "' does not support reasoning. Set reasoning to 'off' or use an OpenAI reasoning model.");
        }

        Response response = client.responses().create(builder.build());

        for (ResponseOutputItem item : response.output()) {
            if (item.isMessage()) {
                ResponseOutputMessage msg = item.asMessage();
                for (ResponseOutputMessage.Content content : msg.content()) {
                    if (content.isOutputText()) {
                        String text = content.asOutputText().text();
                        if (text != null && !text.isBlank()) {
                            return new LlmResponse(text.trim(), tokenUsage(response), null, requestTrace());
                        }
                    }
                }
            }
        }

        return new LlmResponse(response.toString(), tokenUsage(response), null, requestTrace());
    }

    private TokenUsage tokenUsage(Response response) {
        return response.usage()
                .map(this::tokenUsage)
                .orElse(new TokenUsage(null, null, null));
    }

    private TokenUsage tokenUsage(ResponseUsage usage) {
        return new TokenUsage(usage.inputTokens(), usage.outputTokens(), usage.totalTokens());
    }

    static boolean supportsReasoning(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalized = model.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4")
                || normalized.startsWith("gpt-5");
    }

    static boolean sendsSamplingParameters(LlmRequestConfig config) {
        return !(config.sendReasoning() && supportsReasoning(config.model()) && config.reasoning() != null
                && config.reasoning() != Reasoning.OFF);
    }

    private static String reasoningValue(LlmRequestConfig config) {
        if (config.reasoningProviderValue() != null && !config.reasoningProviderValue().isBlank()) {
            return config.reasoningProviderValue();
        }
        return config.reasoning().openAiReasoningEffort();
    }

    private static RequestTrace requestTrace() {
        return RequestTrace.of("https://api.openai.com/v1/responses", Map.of(
                "Content-Type", List.of("application/json")
        ));
    }
}
