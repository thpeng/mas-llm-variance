package ch.thp.mas.llm.variance.analyze.route;

import java.util.List;

public record RouteExtraction(
        int responseIndex,
        String rawResponse,
        List<String> rawExtractedNames,
        List<Destination> normalizedRoute,
        RouteExtractionStatus extractionStatus,
        List<String> unknownNames
) {
}
