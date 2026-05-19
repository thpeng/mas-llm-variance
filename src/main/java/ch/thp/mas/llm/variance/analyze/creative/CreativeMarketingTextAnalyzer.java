package ch.thp.mas.llm.variance.analyze.creative;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CreativeMarketingTextAnalyzer {

    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+(?=\\s|$)");

    public CreativeMarketingTextAnalysis analyze(
            List<String> responses,
            CreativeMarketingTextConfig config,
            CreativeSyntacticAnalysisFactory syntacticFactory
    ) {
        List<CreativeMarketingTextExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config));
        }

        List<Integer> successIndices = extractions.stream()
                .filter(extraction -> extraction.status() == CreativeMarketingTextStatus.SUCCESS)
                .map(CreativeMarketingTextExtraction::responseIndex)
                .toList();
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.status() == CreativeMarketingTextStatus.OUTLIER)
                .map(CreativeMarketingTextExtraction::responseIndex)
                .toList();

        return new CreativeMarketingTextAnalysis(
                responses.size(),
                successIndices.size(),
                outliers.size(),
                responses.isEmpty() ? 0.0 : (double) successIndices.size() / responses.size(),
                outliers,
                config.expectedSentenceCount(),
                config.requiredTerm(),
                count(extractions, extraction -> extraction.sentenceCount() != extraction.expectedSentenceCount()),
                count(extractions, extraction -> !extraction.containsRequiredTerm()),
                extractions,
                syntacticFactory.create(successIndices)
        );
    }

    CreativeMarketingTextExtraction extract(int responseIndex, String response, CreativeMarketingTextConfig config) {
        String rawResponse = response == null ? "" : response;
        String rawTrimmed = rawResponse.trim();
        int sentenceCount = sentenceCount(rawResponse);
        boolean containsRequiredTerm = rawResponse.contains(config.requiredTerm());

        List<String> failureReasons = new ArrayList<>();
        if (sentenceCount != config.expectedSentenceCount()) {
            failureReasons.add("sentence_count_mismatch");
        }
        if (!containsRequiredTerm) {
            failureReasons.add("required_term_missing");
        }

        return new CreativeMarketingTextExtraction(
                responseIndex,
                rawResponse,
                rawTrimmed,
                sentenceCount,
                config.expectedSentenceCount(),
                containsRequiredTerm,
                config.requiredTerm(),
                failureReasons.isEmpty() ? CreativeMarketingTextStatus.SUCCESS : CreativeMarketingTextStatus.OUTLIER,
                List.copyOf(failureReasons)
        );
    }

    int sentenceCount(String response) {
        int count = 0;
        var matcher = SENTENCE_END.matcher(response);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private int count(
            List<CreativeMarketingTextExtraction> extractions,
            java.util.function.Predicate<CreativeMarketingTextExtraction> predicate
    ) {
        return (int) extractions.stream().filter(predicate).count();
    }

    @FunctionalInterface
    public interface CreativeSyntacticAnalysisFactory {
        ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis create(List<Integer> successIndices);
    }
}
