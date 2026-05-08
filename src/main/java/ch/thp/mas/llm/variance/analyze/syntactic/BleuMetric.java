package ch.thp.mas.llm.variance.analyze.syntactic;


import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BleuMetric {

    private final TextTokenizer tokenizer;

    public BleuMetric(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Computes sentence-level BLEU with modified n-gram precision, brevity penalty,
     * and Chen & Cherry (2014) Method 1 smoothing.
     *
     * <p>For n-gram orders with zero matched counts, {@code smoothingEpsilon} is
     * used as the numerator while the denominator remains unchanged. Non-zero
     * modified precisions are left unchanged. This corresponds to the smoothing
     * behaviour used by NLTK's {@code SmoothingFunction.method1}, whose default
     * epsilon is {@code 0.1}.</p>
     *
     * <p>The maximum n-gram order is capped by the candidate length, so empty
     * candidate n-gram orders do not contribute to the score.</p>
     *
     * @param candidate generated text to evaluate
     * @param reference reference text to compare against
     * @param config BLEU configuration, including max n-gram order and smoothing epsilon
     * @return sentence-level BLEU score in the range {@code [0.0, 1.0]}
     * @see <a href="https://aclanthology.org/W14-3346/">Chen and Cherry (2014), A Systematic Comparison of Smoothing Techniques for Sentence-Level BLEU</a>
     * @see <a href="https://www.nltk.org/_modules/nltk/translate/bleu_score.html">NLTK bleu_score.py, SmoothingFunction.method1</a>
     */
    public double score(String candidate, String reference, BleuConfig config) {
        List<String> candidateTokens = tokenizer.tokenize(candidate);
        List<String> referenceTokens = tokenizer.tokenize(reference);
        if (candidateTokens.isEmpty()) {
            return 0.0;
        }

        int effectiveMaxN = Math.min(config.maxN(), candidateTokens.size());
        if (effectiveMaxN == 0) {
            return 0.0;
        }

        double logPrecisionSum = 0.0;
        for (int n = 1; n <= effectiveMaxN; n++) {
            Counts counts = modifiedPrecision(candidateTokens, referenceTokens, n);
            if (counts.total == 0) {
                // No n-grams of this order in candidate; skip (cannot contribute).
                // Reachable only if effectiveMaxN logic is bypassed; defensive guard.
                return 0.0;
            }
            double numerator = counts.matches == 0 ? config.smoothingEpsilon() : counts.matches;
            double precision = numerator / counts.total;
            logPrecisionSum += Math.log(precision);
        }

        double brevityPenalty = candidateTokens.size() > referenceTokens.size()
                ? 1.0
                : Math.exp(1.0 - ((double) referenceTokens.size() / candidateTokens.size()));
        return brevityPenalty * Math.exp(logPrecisionSum / effectiveMaxN);
    }

    private Counts modifiedPrecision(List<String> candidate, List<String> reference, int n) {
        Map<String, Integer> candidateCounts = ngrams(candidate, n);
        Map<String, Integer> referenceCounts = ngrams(reference, n);
        int matches = 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : candidateCounts.entrySet()) {
            int count = entry.getValue();
            total += count;
            matches += Math.min(count, referenceCounts.getOrDefault(entry.getKey(), 0));
        }
        return new Counts(matches, total);
    }

    private Map<String, Integer> ngrams(List<String> tokens, int n) {
        Map<String, Integer> counts = new HashMap<>();
        if (tokens.size() < n) {
            return counts;
        }
        for (int i = 0; i <= tokens.size() - n; i++) {
            String ngram = String.join(" ", tokens.subList(i, i + n));
            counts.merge(ngram, 1, Integer::sum);
        }
        return counts;
    }

    private record Counts(int matches, int total) {
    }
}
