package ch.thp.mas.llm.variance.analyze.route;

import java.util.List;

public record PositionDistribution(
        int position,
        List<PositionDestinationFrequency> frequencies
) {
}
