package ch.thp.mas.llm.variance.metanalysis;

import java.util.List;

public record RoundTripLanguageDestinationExport(
        List<RoundTripLanguageDestinationRow> destinationRows,
        List<RoundTripBfsLanguageRegionRow> bfsLanguageRegionRows
) {
}
