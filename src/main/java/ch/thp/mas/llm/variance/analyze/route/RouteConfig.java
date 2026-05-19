package ch.thp.mas.llm.variance.analyze.route;

public record RouteConfig(int expectedStationCount) {

    public RouteConfig {
        if (expectedStationCount < 1) {
            throw new IllegalArgumentException("expectedStationCount must be at least 1");
        }
    }
}
