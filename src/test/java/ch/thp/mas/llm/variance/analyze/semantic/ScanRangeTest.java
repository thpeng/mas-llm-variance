package ch.thp.mas.llm.variance.analyze.semantic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.AnalysisException;
import org.junit.jupiter.api.Test;

class ScanRangeTest {

    @Test
    void producesInclusiveValues() {
        ScanRange range = ScanRange.of(0.03, 0.06, 0.01, "analysis.hierarchical.threshold");

        assertThat(range.values(0.01)).containsExactly(0.03, 0.04, 0.05, 0.06);
    }

    @Test
    void supportsDegenerateSingleValueScan() {
        ScanRange range = ScanRange.of(0.06, 0.06, 0.01, "analysis.dbscan.epsilon");

        assertThat(range.values(0.01)).containsExactly(0.06);
    }

    @Test
    void supportsNonDefaultIncrementRelativeToFrom() {
        ScanRange range = ScanRange.of(0.03, 0.07, 0.02, "analysis.hierarchical.threshold");

        assertThat(range.values(0.02)).containsExactly(0.03, 0.05, 0.07);
    }

    @Test
    void rejectsNegativeBounds() {
        assertThatThrownBy(() -> ScanRange.of(-0.01, 0.06, 0.01, "analysis.dbscan.epsilon"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void rejectsFromGreaterThanTo() {
        assertThatThrownBy(() -> ScanRange.of(0.07, 0.06, 0.01, "analysis.dbscan.epsilon"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("less than or equal");
    }

    @Test
    void rejectsBoundsNotRepresentableAsHundredths() {
        assertThatThrownBy(() -> ScanRange.of(0.055, 0.06, 0.01, "analysis.dbscan.epsilon"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("hundredths");
    }

    @Test
    void rejectsToBoundNotAlignedToIncrement() {
        assertThatThrownBy(() -> ScanRange.of(0.03, 0.06, 0.02, "analysis.dbscan.epsilon"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("scanIncrement");
    }
}
