package ch.thp.mas.llm.variance.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseUsage;

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

        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        if (config.topP() != null) {
            builder.topP(config.topP());
        }
        if (config.reasoning() != null) {
            builder.reasoning(com.openai.models.Reasoning.builder()
                    .effort(ReasoningEffort.of(config.reasoning().openAiReasoningEffort()))
                    .build());
        }

        Response response = client.responses().create(builder.build());

        for (ResponseOutputItem item : response.output()) {
            if (item.isMessage()) {
                ResponseOutputMessage msg = item.asMessage();
                for (ResponseOutputMessage.Content content : msg.content()) {
                    if (content.isOutputText()) {
                        String text = content.asOutputText().text();
                        if (text != null && !text.isBlank()) {
                            return new LlmResponse(text.trim(), tokenUsage(response));
                        }
                    }
                }
            }
        }

        return new LlmResponse(response.toString(), tokenUsage(response));
    }

    private TokenUsage tokenUsage(Response response) {
        return response.usage()
                .map(this::tokenUsage)
                .orElse(new TokenUsage(null, null, null));
    }

    private TokenUsage tokenUsage(ResponseUsage usage) {
        return new TokenUsage(usage.inputTokens(), usage.outputTokens(), usage.totalTokens());
    }
}
