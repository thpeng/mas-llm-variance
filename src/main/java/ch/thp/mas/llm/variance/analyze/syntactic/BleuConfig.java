package ch.thp.mas.llm.variance.analyze.syntactic;

/**
 * Configuration parameters for {@link BleuMetric}.
 *
 * <p>The metric implements sentence-level BLEU with Chen & Cherry (2014)
 * Method 1 smoothing: when the modified n-gram precision for an order has
 * zero matches, the numerator is replaced by {@code smoothingEpsilon};
 * non-zero precisions are left unchanged.
 *
 * @param maxN             maximum n-gram order, inclusive. Standard BLEU-4
 *                         uses {@code maxN = 4}. Must be at least {@code 1}.
 *                         If a candidate is shorter than {@code maxN} tokens,
 *                         {@link BleuMetric} reduces the effective order
 *                         accordingly.
 * @param smoothingEpsilon epsilon value used by Method 1 smoothing for orders with
 *                         zero matches. Must lie strictly in {@code (0, 1)}.
 *                         Chen & Cherry and NLTK's
 *                         {@code SmoothingFunction.method1} both default to
 *                         {@code 0.1}.
 *
 * @see BleuMetric
 */
public record BleuConfig(int maxN, double smoothingEpsilon) {
    public BleuConfig {
        if (smoothingEpsilon <= 0 || smoothingEpsilon >= 1) {
            throw new IllegalArgumentException("epsilon must be in (0, 1)");
        }
        if (maxN < 1) {
            throw new IllegalArgumentException("maxN must be at least 1");
        }
    }
}
