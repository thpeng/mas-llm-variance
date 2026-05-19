package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.List;

public record SwissRoundTripExtraction(
        int responseIndex,
        String rawResponse,
        List<String> rawExtractedNames,
        List<Destination> normalizedRoundTrip,
        SwissRoundTripExtractionStatus extractionStatus,
        List<String> unknownNames
) {
}
