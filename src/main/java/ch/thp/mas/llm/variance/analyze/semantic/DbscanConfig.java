package ch.thp.mas.llm.variance.analyze.semantic;

/**
 * Configuration parameters for {@link DbscanClusterer}.
 *
 * @param epsilon scan range for the maximum distance (inclusive) at which two points are
 *                considered neighbors. A candidate {@code j} is in the
 *                epsilon-neighborhood of {@code i} iff
 *                {@code distances[i][j] <= epsilon}. The unit and scale of
 *                {@code epsilon} must match the distance metric used to
 *                produce the input matrix (e.g. cosine distance in
 *                {@code [0, 2]}, ROUGE-L complement distance in {@code [0, 1]}).
 * @param minPts  minimum size of an epsilon-neighborhood for a point to qualify as
 *                a core point. The neighborhood count <em>includes the point
 *                itself</em>, so {@code minPts = 1} causes every point to be
 *                its own cluster and effectively disables noise detection.
 *                Common choices are {@code minPts >= 2}; for low-dimensional
 *                data the heuristic {@code minPts = 2 * dim} is often cited
 *                (Ester et al., 1996; Sander et al., 1998).
 */
public record DbscanConfig(ScanRange epsilon, int minPts) {

    public DbscanConfig {
        java.util.Objects.requireNonNull(epsilon, "epsilon must not be null");
        if (minPts < 1) {
            throw new IllegalArgumentException("minPts must be at least 1");
        }
    }
}
