package ch.thp.mas.llm.variance.analyze.factual;

import java.util.List;

public record FactualTravelInfoExtraction(
        int responseIndex,
        String rawResponse,
        List<String> normalizedTimes,
        List<String> extraTimes,
        boolean departureFound,
        boolean arrivalFound,
        boolean changesFound,
        Integer changes,
        String detectedChangeExpression,
        FactualTravelInfoStatus status,
        List<String> failureReasons
) {
}
