package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

public record SwissRoundTripConfig(int expectedStationCount) {

    public SwissRoundTripConfig {
        if (expectedStationCount < 1) {
            throw new IllegalArgumentException("expectedStationCount must be at least 1");
        }
    }
}
