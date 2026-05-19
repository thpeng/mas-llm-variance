package ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TravelerGuidanceFormatEvaluator {

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

    public TravelerGuidanceFormatEvaluation analyze(
            List<String> responses,
            TravelerGuidanceFormatConfig config
    ) {
        List<TravelerGuidanceFormatExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config));
        }

        int exactMatchCount = count(extractions, TravelerGuidanceFormatExtraction::exactMatch);
        int normalizedExactMatchCount = (int) extractions.stream()
                .filter(extraction -> extraction.classification()
                        == TravelerGuidanceFormatClassification.NORMALIZED_EXACT_MATCH)
                .count();
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.classification() == TravelerGuidanceFormatClassification.NO_MATCH)
                .map(TravelerGuidanceFormatExtraction::responseIndex)
                .toList();

        return new TravelerGuidanceFormatEvaluation(
                responses.size(),
                exactMatchCount,
                normalizedExactMatchCount,
                outliers.size(),
                responses.isEmpty() ? 0.0 : (double) exactMatchCount / responses.size(),
                responses.isEmpty() ? 0.0 : (double) (exactMatchCount + normalizedExactMatchCount) / responses.size(),
                outliers,
                forbiddenTemplateTermCounts(extractions),
                count(extractions, TravelerGuidanceFormatExtraction::containsAdditionalSentenceCandidate),
                extractions
        );
    }

    TravelerGuidanceFormatExtraction extract(
            int responseIndex,
            String response,
            TravelerGuidanceFormatConfig config
    ) {
        String rawResponse = response == null ? "" : response;
        String rawTrimmed = rawResponse.trim();
        boolean exactMatch = rawTrimmed.equals(config.reference());
        boolean normalizedExactMatch = normalizeWhitespace(rawTrimmed).equals(normalizeWhitespace(config.reference()));
        TravelerGuidanceFormatClassification classification;
        if (exactMatch) {
            classification = TravelerGuidanceFormatClassification.EXACT_MATCH;
        } else if (normalizedExactMatch) {
            classification = TravelerGuidanceFormatClassification.NORMALIZED_EXACT_MATCH;
        } else {
            classification = TravelerGuidanceFormatClassification.NO_MATCH;
        }

        List<String> forbiddenTerms = forbiddenTerms(rawResponse);
        return new TravelerGuidanceFormatExtraction(
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

    private Map<String, Integer> forbiddenTemplateTermCounts(List<TravelerGuidanceFormatExtraction> extractions) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TravelerGuidanceFormatExtraction extraction : extractions) {
            for (String term : extraction.forbiddenTemplateTerms()) {
                counts.merge(term, 1, Integer::sum);
            }
        }
        return counts;
    }

    private int count(
            List<TravelerGuidanceFormatExtraction> extractions,
            java.util.function.Predicate<TravelerGuidanceFormatExtraction> predicate
    ) {
        return (int) extractions.stream().filter(predicate).count();
    }
}
