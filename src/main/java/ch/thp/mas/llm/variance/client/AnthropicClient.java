package ch.thp.mas.llm.variance.client;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Usage;
import java.util.List;
import java.util.Map;

public class AnthropicClient implements LlmClient {

    private final com.anthropic.client.AnthropicClient client;

    public AnthropicClient(String apiKey) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(1024)
                .addUserMessage(prompt);

        if (useTemperature(config)) {
            builder.temperature(config.temperature());
        } else if (useTopP(config)) {
            builder.topP(config.topP());
        }
        if (config.topK() != null) {
            builder.topK(config.topK().longValue());
        }

        Message message = client.messages().create(builder.build());

        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                String text = block.asText().text();
                if (text != null && !text.isBlank()) {
                    return new LlmResponse(text.trim(), tokenUsage(message.usage()), null, requestTrace());
                }
            }
        }

        return new LlmResponse(message.toString(), tokenUsage(message.usage()), null, requestTrace());
    }

    private TokenUsage tokenUsage(Usage usage) {
        return TokenUsage.of(usage.inputTokens(), usage.outputTokens());
    }

    static boolean useTemperature(LlmRequestConfig config) {
        return config.temperature() != null && Double.compare(config.temperature(), 0.0) != 0;
    }

    static boolean useTopP(LlmRequestConfig config) {
        return !useTemperature(config) && config.topP() != null;
    }

    private static RequestTrace requestTrace() {
        return RequestTrace.of("https://api.anthropic.com/v1/messages", Map.of(
                "Content-Type", List.of("application/json")
        ));
    }
}
