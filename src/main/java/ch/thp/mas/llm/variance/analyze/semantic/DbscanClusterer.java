package ch.thp.mas.llm.variance.analyze.semantic;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import org.springframework.stereotype.Component;

/**
 * Density-based clustering of points given a precomputed pairwise distance matrix,
 * following the DBSCAN algorithm of Ester et al. (1996).
 *
 * <p>A point is a <em>core point</em> if its epsilon-neighborhood (including itself)
 * contains at least {@code minPts} points. Core points and all points
 * density-reachable from them form a cluster. Points that are neither core nor
 * reachable from a core point are labeled as noise.
 *
 * <p>Output convention (matching scikit-learn's {@code DBSCAN}):
 * <ul>
 *   <li>Cluster labels are non-negative integers starting at {@code 0}.</li>
 *   <li>Noise points are labeled {@code -1}.</li>
 *   <li>Cluster IDs are assigned in the order clusters are discovered, which
 *       depends on input point order.</li>
 * </ul>
 *
 * <p>The epsilon-neighborhood is defined inclusively: a candidate {@code j} is a
 * neighbor of {@code i} iff {@code distances[i][j] <= epsilon}. The point
 * itself is always its own neighbor (distance 0).
 *
 * <p>Border-point assignment is order-dependent: a point reachable from two
 * different clusters is assigned to whichever cluster reaches it first. This
 * matches the original DBSCAN definition. Given a fixed input ordering the
 * result is fully deterministic.
 *
 * <p>Complexity: O(n^2) time and O(n^2) space, dominated by the precomputed
 * distance matrix. No spatial indexing is used, which is appropriate for
 * arbitrary precomputed distances (e.g. cosine distance over LLM embeddings)
 * where indexing would not apply.
 *
 * <p>Reference: Ester, M., Kriegel, H.-P., Sander, J. & Xu, X. (1996).
 * <em>A Density-Based Algorithm for Discovering Clusters in Large Spatial
 * Databases with Noise</em>. KDD-96. Results can be cross-verified against
 * {@code sklearn.cluster.DBSCAN(metric='precomputed')}.
 */
@Component
public class DbscanClusterer {

    private static final int UNVISITED = Integer.MIN_VALUE;
    private static final int NOISE = -1;

    /**
     * Clusters points based on the given precomputed distance matrix.
     *
     * @param distances square symmetric pairwise distance matrix; the diagonal
     *                  is expected to be zero
     * @param config    epsilon (neighborhood radius, inclusive) and minPts
     *                  (minimum neighborhood size for a core point, including
     *                  the point itself)
     * @return an array of length {@code distances.length} where entry {@code i}
     *         is either a non-negative cluster ID or {@code -1} for noise
     */
    public int[] cluster(double[][] distances, DbscanConfig config) {
        return cluster(distances, config.epsilon().from(), config.minPts());
    }

    public int[] cluster(double[][] distances, double epsilon, int minPts) {
        int[] labels = new int[distances.length];
        Arrays.fill(labels, UNVISITED);
        int clusterId = 0;

        for (int point = 0; point < distances.length; point++) {
            if (labels[point] != UNVISITED) {
                continue;
            }
            int[] neighbors = neighbors(distances, point, epsilon);
            if (neighbors.length < minPts) {
                labels[point] = NOISE;
                continue;
            }
            expandCluster(distances, labels, point, neighbors, clusterId, epsilon, minPts);
            clusterId++;
        }
        return labels;
    }

    /**
     * Expands a cluster from a seed core point via BFS over density-reachable
     * neighbors. Border points previously labeled as noise are reassigned to
     * the current cluster.
     */
    private void expandCluster(
            double[][] distances,
            int[] labels,
            int point,
            int[] neighbors,
            int clusterId,
            double epsilon,
            int minPts
    ) {
        labels[point] = clusterId;
        Queue<Integer> queue = new ArrayDeque<>();
        for (int neighbor : neighbors) {
            queue.add(neighbor);
        }

        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (labels[current] == NOISE) {
                labels[current] = clusterId;
            }
            if (labels[current] != UNVISITED) {
                continue;
            }
            labels[current] = clusterId;
            int[] currentNeighbors = neighbors(distances, current, epsilon);
            if (currentNeighbors.length >= minPts) {
                for (int neighbor : currentNeighbors) {
                    queue.add(neighbor);
                }
            }
        }
    }

    /**
     * Returns the indices of all points within {@code epsilon} of {@code point},
     * including {@code point} itself.
     */
    private int[] neighbors(double[][] distances, int point, double epsilon) {
        return java.util.stream.IntStream.range(0, distances.length)
                .filter(candidate -> distances[point][candidate] <= epsilon)
                .toArray();
    }
}
