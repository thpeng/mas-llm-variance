package ch.thp.mas.llm.variance.analyze.literalformat;

import java.util.List;

public record LiteralFormatTravelerGuidanceExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        boolean exactMatch,
        boolean normalizedExactMatch,
        LiteralFormatTravelerGuidanceClassification classification,
        boolean hasBern,
        boolean hasZurich,
        boolean hasBernWankdorf,
        boolean hasS3,
        boolean containsForbiddenTemplateContent,
        List<String> forbiddenTemplateTerms,
        boolean containsAdditionalSentenceCandidate
) {
}
