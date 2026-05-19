package ch.thp.mas.llm.variance.analyze.literal;

import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LiteralAnalyzer {

    public LiteralAnalysis analyze(List<String> responses) {
        if (responses.isEmpty()) {
            return new LiteralAnalysis(false, 0, 0, 0.0);
        }
        int distinctResponseCount = new HashSet<>(responses).size();
        return new LiteralAnalysis(
                distinctResponseCount == 1,
                responses.size(),
                distinctResponseCount,
                exactMatchRate(responses)
        );
    }

    private double exactMatchRate(List<String> responses) {
        int pairCount = 0;
        int exactMatches = 0;
        for (int i = 0; i < responses.size(); i++) {
            for (int j = i + 1; j < responses.size(); j++) {
                pairCount++;
                if (responses.get(i).equals(responses.get(j))) {
                    exactMatches++;
                }
            }
        }
        return pairCount == 0 ? 1.0 : (double) exactMatches / pairCount;
    }
}
