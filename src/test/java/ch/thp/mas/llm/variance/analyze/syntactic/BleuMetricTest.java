package ch.thp.mas.llm.variance.analyze.syntactic;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class BleuMetricTest {

    private final BleuMetric metric = new BleuMetric(new TextTokenizer());

    @Test
    void returnsPositiveScoreForShortTextBecauseOfSmoothing() {
        double score = metric.score("hauptstadt bern", "hauptstadt bern", new BleuConfig(4,  0.1));

        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    void returnsZeroForEmptyCandidate() {
        assertThat(metric.score("", "hauptstadt bern", new BleuConfig(4, 0.1))).isEqualTo(0.0);
    }

    @Test
    void noOverlapStillReturnsSmoothedPositiveScore() {
        double score = metric.score("foo bar", "hauptstadt bern", new BleuConfig(4,  0.1));

        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    void calculateLeavesDeparts() {
        double score = metric.score("the train leaves zurich", "the train departs zurich", new BleuConfig(4,  0.1));

        assertThat(score).isCloseTo(0.188030, Percentage.withPercentage(0.0001));
    }

    @Test
    void brevityPenaltyReducesShortCandidateScore() {
        BleuConfig config = new BleuConfig(4,  0.1);

        double shortCandidate = metric.score("bern", "bern ist hauptstadt", config);
        double equalLengthCandidate = metric.score("bern ist hauptstadt", "bern ist hauptstadt", config);

        assertThat(shortCandidate).isLessThan(equalLengthCandidate);
    }
}
