package ch.thp.mas.llm.variance.analyze.syntactic;


import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computes sentence-level ROUGE-L F1 (beta = 1) between a candidate and a reference text.
 *
 * <p>ROUGE-L is based on the Longest Common Subsequence (LCS) between the two token
 * sequences. Precision is defined as {@code LCS / |candidate|}, recall as
 * {@code LCS / |reference|}, and the returned score as the harmonic mean
 * {@code F1 = 2 * p * r / (p + r)}.
 *
 * <p>The beta = 1 variant matches the de facto community standard (Google
 * {@code rouge_score}, Hugging Face {@code evaluate}, NLTK) and deviates from the
 * recall-weighted default in Lin (2004). Stemming and stopword removal are not
 * applied; case sensitivity is delegated to {@link TextTokenizer}.
 *
 * <p>Edge cases:
 * <ul>
 *   <li>Empty candidate or reference: returns {@code 0.0}.</li>
 *   <li>LCS = 0 (no common tokens): returns {@code 0.0}.</li>
 *   <li>Identical inputs: returns {@code 1.0}.</li>
 * </ul>
 *
 * <p>This is the <em>sentence-level</em> ROUGE-L variant, not ROUGE-Lsum. LCS is
 * computed via classical O(n * m) dynamic programming.
 *
 * <p>Reference: Lin, C.-Y. (2004). <em>ROUGE: A Package for Automatic Evaluation of
 * Summaries</em>. Results can be cross-verified against the Google {@code rouge_score}
 * Python package with {@code use_stemmer=False}.
 */
@Component
public class RougeLMetric {

    private final TextTokenizer tokenizer;

    public RougeLMetric(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Computes the ROUGE-L F1 score between candidate and reference.
     *
     * @param candidate the generated text to evaluate
     * @param reference the reference text to compare against
     * @return F1 score in {@code [0.0, 1.0]}; {@code 0.0} if either input is empty
     * or no tokens overlap
     */
    public double score(String candidate, String reference) {
        List<String> candidateTokens = tokenizer.tokenize(candidate);
        List<String> referenceTokens = tokenizer.tokenize(reference);
        if (candidateTokens.isEmpty() || referenceTokens.isEmpty()) {
            return 0.0;
        }
        int lcs = lcs(candidateTokens, referenceTokens);
        double precision = (double) lcs / candidateTokens.size();
        double recall = (double) lcs / referenceTokens.size();
        if (precision + recall == 0.0) {
            return 0.0;
        }
        return 2 * precision * recall / (precision + recall);
    }


    /**
     * Computes the length of the Longest Common Subsequence of two token lists
     * using O(n * m) time and space dynamic programming.
     */
    private int lcs(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = 1; i <= a.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[a.size()][b.size()];
    }
}
