package ch.thp.mas.llm.variance.analyze.factual;

import java.util.Objects;

public record FactualTravelInfoConfig(
        String departureFromBern,
        String arrivalAtZurich,
        int changes
) {

    public FactualTravelInfoConfig {
        Objects.requireNonNull(departureFromBern, "departureFromBern must not be null");
        Objects.requireNonNull(arrivalAtZurich, "arrivalAtZurich must not be null");
        if (changes < 0) {
            throw new IllegalArgumentException("changes must not be negative");
        }
    }
}
