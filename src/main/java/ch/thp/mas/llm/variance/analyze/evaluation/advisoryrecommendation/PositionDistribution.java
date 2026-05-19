package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.List;

public record PositionDistribution(
        int position,
        List<PositionDestinationFrequency> frequencies
) {
}
