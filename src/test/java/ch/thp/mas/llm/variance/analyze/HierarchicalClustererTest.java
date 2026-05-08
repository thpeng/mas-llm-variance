package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HierarchicalClustererTest {

    @Test
    void completeLinkagePreventsChainMerging() {
        double[][] distances = {
                {0.0, 0.04, 0.08},
                {0.04, 0.0, 0.04},
                {0.08, 0.04, 0.0}
        };

        int[] labels = new HierarchicalClusterer().cluster(
                distances,
                new HierarchicalConfig(0.05, HierarchicalLinkage.COMPLETE)
        );

        assertThat(labels).containsExactly(0, 0, 1);
    }

    @Test
    void averageLinkageCanMergeWhenAverageDistanceFitsThreshold() {
        double[][] distances = {
                {0.0, 0.04, 0.08},
                {0.04, 0.0, 0.04},
                {0.08, 0.04, 0.0}
        };

        int[] labels = new HierarchicalClusterer().cluster(
                distances,
                new HierarchicalConfig(0.06, HierarchicalLinkage.AVERAGE)
        );

        assertThat(labels).containsExactly(0, 0, 0);
    }
}
