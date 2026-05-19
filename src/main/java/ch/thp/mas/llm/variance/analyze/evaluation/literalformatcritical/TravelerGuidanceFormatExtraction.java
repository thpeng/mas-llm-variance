package ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical;

import java.util.List;

public record TravelerGuidanceFormatExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        boolean exactMatch,
        boolean normalizedExactMatch,
        TravelerGuidanceFormatClassification classification,
        boolean hasBern,
        boolean hasZurich,
        boolean hasBernWankdorf,
        boolean hasS3,
        boolean containsForbiddenTemplateContent,
        List<String> forbiddenTemplateTerms,
        boolean containsAdditionalSentenceCandidate
) {
}
