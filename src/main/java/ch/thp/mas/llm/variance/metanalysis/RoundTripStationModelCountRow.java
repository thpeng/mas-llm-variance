package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;

public record RoundTripStationModelCountRow(
        String model,
        Destination destination,
        int mentionCount,
        int modelStationMentionCount,
        double shareOfModelStationMentions,
        int contributingSeriesCount
) {
}
