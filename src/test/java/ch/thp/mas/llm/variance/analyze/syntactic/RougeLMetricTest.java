package ch.thp.mas.llm.variance.analyze.syntactic;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import org.junit.jupiter.api.Test;

class RougeLMetricTest {

    private final RougeLMetric metric = new RougeLMetric(new TextTokenizer());

    @Test
    void returnsOneForEqualTexts() {
        assertThat(metric.score("die schweiz ist schoen", "die schweiz ist schoen")).isEqualTo(1.0);
    }

    @Test
    void returnsPartialLcsF1() {
        assertThat(metric.score("a b c", "a x c")).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void returnsZeroForNoOverlapOrEmptyInput() {
        assertThat(metric.score("a b c", "x y z")).isEqualTo(0.0);
        assertThat(metric.score("", "x y z")).isEqualTo(0.0);
        assertThat(metric.score("a b c", "")).isEqualTo(0.0);
    }

    @Test
    void sameTokensInDifferentOrderAreOnlyPartialMatch() {
        assertThat(metric.score("a b c", "c b a")).isCloseTo(1.0 / 3.0, org.assertj.core.data.Offset.offset(0.0001));
    }
}
