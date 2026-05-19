package ch.thp.mas.llm.variance.analyze.evaluation.factualcritical;

import java.util.List;

public record BernZurichConnectionExtraction(
        int responseIndex,
        String rawResponse,
        List<String> normalizedTimes,
        List<String> extraTimes,
        boolean departureFound,
        boolean arrivalFound,
        boolean changesFound,
        Integer changes,
        String detectedChangeExpression,
        BernZurichConnectionStatus status,
        List<String> failureReasons
) {
}
