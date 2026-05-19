package ch.thp.mas.llm.variance.analyze.evaluation.factualcritical;

import java.util.Objects;

public record BernZurichConnectionConfig(
        String departureFromBern,
        String arrivalAtZurich,
        int changes
) {

    public BernZurichConnectionConfig {
        Objects.requireNonNull(departureFromBern, "departureFromBern must not be null");
        Objects.requireNonNull(arrivalAtZurich, "arrivalAtZurich must not be null");
        if (changes < 0) {
            throw new IllegalArgumentException("changes must not be negative");
        }
    }
}
