package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.Objects;

public record SwissRoundTripConfig(int expectedStationCount, PromptLanguage language) {

    public SwissRoundTripConfig(int expectedStationCount) {
        this(expectedStationCount, PromptLanguage.DE);
    }

    public SwissRoundTripConfig {
        if (expectedStationCount < 1) {
            throw new IllegalArgumentException("expectedStationCount must be at least 1");
        }
        Objects.requireNonNull(language, "language must not be null");
    }
}
