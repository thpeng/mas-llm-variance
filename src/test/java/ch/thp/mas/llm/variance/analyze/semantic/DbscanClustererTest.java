package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DbscanClustererTest {

    @Test
    void clustersByDistanceMatrixAndMarksOutliers() {
        double[][] distances = {
                {0.0, 0.1, 0.9},
                {0.1, 0.0, 0.9},
                {0.9, 0.9, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.2, 2));

        assertThat(labels).containsExactly(0, 0, -1);
    }

    @Test
    void createsTwoDenseClusters() {
        double[][] distances = {
                {0.0, 0.1, 0.8, 0.9},
                {0.1, 0.0, 0.9, 0.8},
                {0.8, 0.9, 0.0, 0.1},
                {0.9, 0.8, 0.1, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.2, 2));

        assertThat(labels).containsExactly(0, 0, 1, 1);
    }

    @Test
    void marksAllPointsAsNoiseWhenNoPointIsDenseEnough() {
        double[][] distances = {
                {0.0, 0.8, 0.9},
                {0.8, 0.0, 0.9},
                {0.9, 0.9, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.2, 2));

        assertThat(labels).containsExactly(-1, -1, -1);
    }

    @Test
    void clustersOnlyIdenticalPointsWhenEpsilonIsZero() {
        double[][] distances = {
                {0.0, 0.0, 0.5},
                {0.0, 0.0, 0.5},
                {0.5, 0.5, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.0, 2));

        assertThat(labels).containsExactly(0, 0, -1);
    }

    @Test
    void minPtsOneTurnsEveryUnvisitedPointIntoACluster() {
        double[][] distances = {
                {0.0, 0.8, 0.9},
                {0.8, 0.0, 0.9},
                {0.9, 0.9, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.2, 1));

        assertThat(labels).containsExactly(0, 1, 2);
    }
}
