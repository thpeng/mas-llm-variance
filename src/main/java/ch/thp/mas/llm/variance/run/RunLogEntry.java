package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.TokenUsage;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunLogEntry(
        int index,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long seed,
        String requestUrl,
        Map<String, List<String>> requestHeaders,
        String response,
        TokenUsage tokenUsage
) {

    public RunLogEntry {
        requestHeaders = copyHeaders(requestHeaders);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Long seed,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, seed, null, null, response, tokenUsage);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, null, null, null, response, tokenUsage);
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        return headers.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }
}
