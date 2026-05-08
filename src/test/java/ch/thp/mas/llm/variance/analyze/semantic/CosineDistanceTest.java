package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.EmbeddingResult;
import org.junit.jupiter.api.Test;

class CosineDistanceTest {

    @Test
    void computesCosineDistance() {
        CosineDistance distance = new CosineDistance();

        assertThat(distance.distance(new double[]{1, 0}, new double[]{1, 0})).isEqualTo(0.0);
        assertThat(distance.distance(new double[]{1, 0}, new double[]{0, 1})).isEqualTo(1.0);
        assertThat(distance.distance(new double[]{1, 0}, new double[]{-1, 0})).isEqualTo(2.0);
        assertThat(distance.distance(new double[]{2, 0}, new double[]{4, 0})).isEqualTo(0.0);
        assertThat(distance.distance(new double[]{0, 0}, new double[]{1, 0})).isEqualTo(1.0);
    }

    @Test
    void pairwiseMatrixIsSymmetricWithZeroDiagonal() {
        CosineDistance distance = new CosineDistance();

        double[][] matrix = distance.pairwise(java.util.List.of(
                new EmbeddingResult(new double[]{1, 0}, false),
                new EmbeddingResult(new double[]{0, 1}, false),
                new EmbeddingResult(new double[]{-1, 0}, false)
        ));

        assertThat(matrix).hasDimensions(3, 3);
        assertThat(matrix[0][0]).isEqualTo(0.0);
        assertThat(matrix[1][1]).isEqualTo(0.0);
        assertThat(matrix[2][2]).isEqualTo(0.0);
        assertThat(matrix[0][1]).isEqualTo(matrix[1][0]);
        assertThat(matrix[0][2]).isEqualTo(2.0);
    }
}
