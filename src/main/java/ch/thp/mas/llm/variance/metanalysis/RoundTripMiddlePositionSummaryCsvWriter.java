package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoundTripMiddlePositionSummaryCsvWriter {

    private static final Path DEFAULT_OUTPUT = Path.of(
            "src",
            "main",
            "resources",
            "metanalysis",
            "1008-main-roundtrip-de-mittel-position-summary.csv"
    );
    private static final String HEADER = String.join(",",
            "series_id",
            "model",
            "successful_extraction_count",
            "unique_route_count",
            "top_route_share",
            "literal_unique_count",
            "position_1_distinct_count",
            "position_2_distinct_count",
            "position_3_distinct_count",
            "position_4_distinct_count",
            "position_5_distinct_count"
    );

    public Path write(List<RoundTripMiddlePositionSummaryRow> rows, String output) {
        Path target = output == null || output.isBlank() ? DEFAULT_OUTPUT : Path.of(output.trim()).normalize();
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String body = rows.stream()
                    .map(this::csvLine)
                    .collect(Collectors.joining(System.lineSeparator()));
            String csv = HEADER + System.lineSeparator() + body + System.lineSeparator();
            return Files.writeString(
                    target,
                    csv,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not write roundtrip middle position summary CSV: " + target, e);
        }
    }

    private String csvLine(RoundTripMiddlePositionSummaryRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.model()),
                csv(row.successfulExtractionCount()),
                csv(row.uniqueRouteCount()),
                csv(row.topRouteShare()),
                csv(row.literalUniqueCount()),
                csv(row.position1DistinctCount()),
                csv(row.position2DistinctCount()),
                csv(row.position3DistinctCount()),
                csv(row.position4DistinctCount()),
                csv(row.position5DistinctCount())
        );
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String string = value.toString();
        if (string.contains(",") || string.contains("\"") || string.contains("\n") || string.contains("\r")) {
            return "\"" + string.replace("\"", "\"\"") + "\"";
        }
        return string;
    }
}
