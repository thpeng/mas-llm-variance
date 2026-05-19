package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.TokenUsage;
import java.time.OffsetDateTime;

public record RunLogEntry(
        int index,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long seed,
        String response,
        TokenUsage tokenUsage
) {

    public RunLogEntry(
            int index,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String response,
            TokenUsage tokenUsage
    ) {
        this(index, startedAt, endedAt, null, response, tokenUsage);
    }
}
