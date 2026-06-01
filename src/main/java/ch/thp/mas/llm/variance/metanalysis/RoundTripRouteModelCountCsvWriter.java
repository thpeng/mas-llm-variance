package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoundTripRouteModelCountCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String DEFAULT_FILENAME = "1010-roundtrip-route-model-counts.csv";
    private static final String HEADER = String.join(",",
            "model",
            "route_key",
            "route_stations",
            "mention_count",
            "model_route_count",
            "share_of_model_routes",
            "contributing_series_count"
    );

    public Path write(List<RoundTripRouteModelCountRow> rows, String output) {
        Path target = output == null || output.isBlank()
                ? DEFAULT_OUTPUT_DIRECTORY.resolve(DEFAULT_FILENAME).normalize()
                : Path.of(output.trim()).normalize();
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
            throw new MetaAnalysisException("Could not write roundtrip route model count CSV: " + target, e);
        }
    }

    private String csvLine(RoundTripRouteModelCountRow row) {
        return String.join(",",
                csv(row.model()),
                csv(row.routeKey()),
                csv(row.routeStations()),
                csv(row.mentionCount()),
                csv(row.modelRouteCount()),
                csv(row.shareOfModelRoutes()),
                csv(row.contributingSeriesCount())
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
