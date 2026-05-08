package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MedoidSelectorTest {

    private final MedoidSelector selector = new MedoidSelector();

    @Test
    void selectsSingleResponse() {
        Medoid medoid = selector.select(new double[][]{{0.0}});

        assertThat(medoid.index()).isEqualTo(0);
        assertThat(medoid.totalDistance()).isEqualTo(0.0);
    }

    @Test
    void selectsResponseWithMinimalTotalDistance() {
        double[][] distances = {
                {0.0, 0.1, 0.9},
                {0.1, 0.0, 0.8},
                {0.9, 0.8, 0.0}
        };

        Medoid medoid = selector.select(distances);

        assertThat(medoid.index()).isEqualTo(1);
        assertThat(medoid.totalDistance()).isEqualTo(0.9);
    }

    @Test
    void tieSelectsLowestIndex() {
        double[][] distances = {
                {0.0, 0.5},
                {0.5, 0.0}
        };

        Medoid medoid = selector.select(distances);

        assertThat(medoid.index()).isEqualTo(0);
    }
}
