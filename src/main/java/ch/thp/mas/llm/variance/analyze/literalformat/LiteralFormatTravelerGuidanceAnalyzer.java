package ch.thp.mas.llm.variance.analyze.literalformat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LiteralFormatTravelerGuidanceAnalyzer {

    private static final List<String> FORBIDDEN_TERMS = List.of(
            "Sargans",
            "Chur",
            "Landquart",
            "Muttenz",
            "Basel",
            "St. Jakob",
            "RE5",
            "S12",
            "S1",
            "47",
            "EV"
    );
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?](?=\\s|$)");

    public LiteralFormatTravelerGuidanceAnalysis analyze(
            List<String> responses,
            LiteralFormatTravelerGuidanceConfig config
    ) {
        List<LiteralFormatTravelerGuidanceExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config));
        }

        int exactMatchCount = count(extractions, LiteralFormatTravelerGuidanceExtraction::exactMatch);
        int normalizedExactMatchCount = (int) extractions.stream()
                .filter(extraction -> extraction.classification()
                        == LiteralFormatTravelerGuidanceClassification.NORMALIZED_EXACT_MATCH)
                .count();
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.classification() == LiteralFormatTravelerGuidanceClassification.NO_MATCH)
                .map(LiteralFormatTravelerGuidanceExtraction::responseIndex)
                .toList();

        return new LiteralFormatTravelerGuidanceAnalysis(
                responses.size(),
                exactMatchCount,
                normalizedExactMatchCount,
                outliers.size(),
                responses.isEmpty() ? 0.0 : (double) exactMatchCount / responses.size(),
                responses.isEmpty() ? 0.0 : (double) (exactMatchCount + normalizedExactMatchCount) / responses.size(),
                outliers,
                forbiddenTemplateTermCounts(extractions),
                count(extractions, LiteralFormatTravelerGuidanceExtraction::containsAdditionalSentenceCandidate),
                extractions
        );
    }

    LiteralFormatTravelerGuidanceExtraction extract(
            int responseIndex,
            String response,
            LiteralFormatTravelerGuidanceConfig config
    ) {
        String rawResponse = response == null ? "" : response;
        String rawTrimmed = rawResponse.trim();
        boolean exactMatch = rawTrimmed.equals(config.reference());
        boolean normalizedExactMatch = normalizeWhitespace(rawTrimmed).equals(normalizeWhitespace(config.reference()));
        LiteralFormatTravelerGuidanceClassification classification;
        if (exactMatch) {
            classification = LiteralFormatTravelerGuidanceClassification.EXACT_MATCH;
        } else if (normalizedExactMatch) {
            classification = LiteralFormatTravelerGuidanceClassification.NORMALIZED_EXACT_MATCH;
        } else {
            classification = LiteralFormatTravelerGuidanceClassification.NO_MATCH;
        }

        List<String> forbiddenTerms = forbiddenTerms(rawResponse);
        return new LiteralFormatTravelerGuidanceExtraction(
                responseIndex,
                rawResponse,
                rawTrimmed,
                exactMatch,
                normalizedExactMatch,
                classification,
                rawResponse.contains("Bern"),
                rawResponse.contains("Zürich"),
                rawResponse.contains("Bern Wankdorf"),
                tokenContains(rawResponse, "S3"),
                !forbiddenTerms.isEmpty(),
                forbiddenTerms,
                sentenceCount(rawResponse) > 1
        );
    }

    String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private List<String> forbiddenTerms(String response) {
        return FORBIDDEN_TERMS.stream()
                .filter(term -> tokenContains(response, term))
                .toList();
    }

    private boolean tokenContains(String response, String term) {
        return Pattern.compile("(?<![\\p{Alnum}])" + Pattern.quote(term) + "(?![\\p{Alnum}])")
                .matcher(response)
                .find();
    }

    private int sentenceCount(String response) {
        int count = 0;
        var matcher = SENTENCE_END.matcher(response);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private Map<String, Integer> forbiddenTemplateTermCounts(List<LiteralFormatTravelerGuidanceExtraction> extractions) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (LiteralFormatTravelerGuidanceExtraction extraction : extractions) {
            for (String term : extraction.forbiddenTemplateTerms()) {
                counts.merge(term, 1, Integer::sum);
            }
        }
        return counts;
    }

    private int count(
            List<LiteralFormatTravelerGuidanceExtraction> extractions,
            java.util.function.Predicate<LiteralFormatTravelerGuidanceExtraction> predicate
    ) {
        return (int) extractions.stream().filter(predicate).count();
    }
}
