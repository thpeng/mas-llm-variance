package ch.thp.mas.llm.variance.analyze.semantic;

import org.springframework.stereotype.Component;

/**
 * Selects the medoid of a set of points given their pairwise distance matrix.
 *
 * <p>The medoid is the point that minimizes the sum of distances to all other
 * points. Unlike a centroid, the medoid is always one of the input points,
 * which makes it suitable for representing a "central" element in domains
 * where intermediate points have no meaning, such as the embedding space of
 * LLM responses.
 *
 * <p>This is the standard PAM-style (Partitioning Around Medoids) selection:
 * argmin over rows of {@code sum(distances[i])}. The diagonal contribution
 * {@code distances[i][i]} is expected to be {@code 0.0} and does not affect
 * the result.
 *
 * <p>Returned value:
 * <ul>
 *   <li>{@link Medoid#index()} is the row/column index of the selected point.</li>
 *   <li>{@link Medoid#bestTotal()} is the <em>sum</em> of distances from the
 *       medoid to all other points (not the mean). The argmin is identical for
 *       sum and mean; downstream code that interprets this value as a
 *       dispersion measure must account for the {@code n}-dependence.</li>
 * </ul>
 *
 * <p>Determinism: in case of ties, the first index achieving the minimum is
 * returned, which is a stable choice for reproducibility.
 *
 * <p>Preconditions: {@code distances} is expected to be a square, symmetric
 * matrix with a zero diagonal. These properties are not validated.
 *
 * <p>Reference: Kaufman, L. & Rousseeuw, P. J. (1990). <em>Finding Groups in
 * Data: An Introduction to Cluster Analysis</em>, chapter on Partitioning
 * Around Medoids (PAM).
 */
@Component
public class MedoidSelector {

    /**
     * Selects the medoid from a pairwise distance matrix.
     *
     * @param distances square symmetric pairwise distance matrix with zero
     *                  diagonal; must contain at least one row
     * @return the medoid index and its summed distance to all other points;
     *         returns {@code Medoid(-1, Double.POSITIVE_INFINITY)} for an
     *         empty matrix
     */
    public Medoid select(double[][] distances) {
        int bestIndex = -1;
        double bestTotal = Double.POSITIVE_INFINITY;
        for (int i = 0; i < distances.length; i++) {
            double total = 0.0;
            for (double distance : distances[i]) {
                total += distance;
            }
            if (total < bestTotal) {
                bestTotal = total;
                bestIndex = i;
            }
        }
        return new Medoid(bestIndex, bestTotal);
    }
}