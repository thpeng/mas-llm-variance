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
        RunLogEntryStatus status,
        Long seed,
        String requestUrl,
        Map<String, List<String>> requestHeaders,
        String requestBody,
        Integer responseStatusCode,
        Map<String, List<String>> responseHeaders,
        String responseBody,
        String response,
        TokenUsage tokenUsage,
        Integer errorStatusCode,
        String errorType,
        String errorMessage,
        String errorBody
) {

    public RunLogEntry {
        status = status == null ? RunLogEntryStatus.SUCCESS : status;
        requestHeaders = copyHeaders(requestHeaders);
        responseHeaders = copyHeaders(responseHeaders);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Long seed,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, RunLogEntryStatus.SUCCESS, seed, null, null, null, null, null, null,
                response, tokenUsage, null, null, null, null);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, RunLogEntryStatus.SUCCESS, null, null, null, null, null, null, null,
                response, tokenUsage, null, null, null, null);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Long seed,
            String requestUrl,
            Map<String, List<String>> requestHeaders,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, RunLogEntryStatus.SUCCESS, seed, requestUrl, requestHeaders, null, null, null,
                null, response, tokenUsage, null, null, null, null);
    }

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Long seed,
            String requestUrl,
            Map<String, List<String>> requestHeaders,
            String requestBody,
            Integer responseStatusCode,
            Map<String, List<String>> responseHeaders,
            String responseBody,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, RunLogEntryStatus.SUCCESS, seed, requestUrl, requestHeaders, requestBody,
                responseStatusCode, responseHeaders, responseBody, response, tokenUsage, null, null, null, null);
    }

    public static RunLogEntry servingError(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Long seed,
            String requestUrl,
            Map<String, List<String>> requestHeaders,
            String requestBody,
            Integer responseStatusCode,
            Map<String, List<String>> responseHeaders,
            String responseBody,
            int errorStatusCode,
            String errorMessage,
            String errorBody
    ) {
        return new RunLogEntry(
                index,
                startedAt,
                endedAt,
                RunLogEntryStatus.SERVING_ERROR,
                seed,
                requestUrl,
                requestHeaders,
                requestBody,
                responseStatusCode,
                responseHeaders,
                responseBody,
                null,
                null,
                errorStatusCode,
                "HTTP_" + errorStatusCode,
                errorMessage,
                errorBody
        );
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
