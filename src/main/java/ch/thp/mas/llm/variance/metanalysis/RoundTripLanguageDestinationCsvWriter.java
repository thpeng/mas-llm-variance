package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoundTripLanguageDestinationCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String DESTINATION_FILENAME = "1004-roundtrip-language-destination-expected-vs-observed.csv";
    private static final String BFS_REGION_FILENAME = "1004-roundtrip-language-destination-bfs-language-region-expected-vs-observed.csv";

    private static final String DESTINATION_HEADER = String.join(",",
            "dataset",
            "provider",
            "model",
            "model_version",
            "prompt_language",
            "plan_name",
            "destination",
            "model_count",
            "model_station_mention_count",
            "expected_probability",
            "series_station_mention_count",
            "expected_count",
            "observed_count",
            "observed_probability",
            "delta_count",
            "delta_probability",
            "observed_expected_ratio",
            "direction"
    );

    private static final String BFS_REGION_HEADER = String.join(",",
            "dataset",
            "provider",
            "model",
            "model_version",
            "prompt_language",
            "plan_name",
            "bfs_language_region",
            "bfs_language_region_label",
            "destinations_in_model_region",
            "series_station_mention_count",
            "expected_count",
            "observed_count",
            "expected_probability",
            "observed_probability",
            "delta_count",
            "delta_probability",
            "observed_expected_ratio",
            "direction"
    );

    public Path write(RoundTripLanguageDestinationExport export, String selection, String output) {
        Path bfsRegionPath = outputPath(output);
        Path destinationPath = companionPath(bfsRegionPath, DESTINATION_FILENAME);
        try {
            writeString(destinationPath, destinationCsv(export.destinationRows()));
            writeString(bfsRegionPath, bfsRegionCsv(export.bfsLanguageRegionRows()));
            return bfsRegionPath;
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not write roundtrip language destination metanalysis CSVs.", e);
        }
    }

    private Path companionPath(Path bfsRegionPath, String filename) {
        Path parent = bfsRegionPath.getParent();
        return parent == null ? Path.of(filename) : parent.resolve(filename);
    }

    private Path outputPath(String output) {
        if (output != null && !output.isBlank()) {
            return Path.of(output.trim()).normalize();
        }
        return DEFAULT_OUTPUT_DIRECTORY.resolve(BFS_REGION_FILENAME).normalize();
    }

    private void writeString(Path target, String csv) throws java.io.IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                target,
                csv,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private String destinationCsv(List<RoundTripLanguageDestinationRow> rows) {
        return DESTINATION_HEADER + System.lineSeparator()
                + rows.stream().map(this::destinationLine).collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
    }

    private String destinationLine(RoundTripLanguageDestinationRow row) {
        return String.join(",",
                csv(row.dataset()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelVersion()),
                csv(row.promptLanguage()),
                csv(row.planName()),
                csv(row.destination()),
                csv(row.modelCount()),
                csv(row.modelStationMentionCount()),
                csv(row.expectedProbability()),
                csv(row.seriesStationMentionCount()),
                csv(row.expectedCount()),
                csv(row.observedCount()),
                csv(row.observedProbability()),
                csv(row.deltaCount()),
                csv(row.deltaProbability()),
                csv(row.observedExpectedRatio()),
                csv(row.direction())
        );
    }

    private String bfsRegionCsv(List<RoundTripBfsLanguageRegionRow> rows) {
        return BFS_REGION_HEADER + System.lineSeparator()
                + rows.stream().map(this::bfsRegionLine).collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
    }

    private String bfsRegionLine(RoundTripBfsLanguageRegionRow row) {
        return String.join(",",
                csv(row.dataset()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelVersion()),
                csv(row.promptLanguage()),
                csv(row.planName()),
                csv(row.bfsLanguageRegion()),
                csv(row.bfsLanguageRegionLabel()),
                csv(row.destinationsInModelRegion()),
                csv(row.seriesStationMentionCount()),
                csv(row.expectedCount()),
                csv(row.observedCount()),
                csv(row.expectedProbability()),
                csv(row.observedProbability()),
                csv(row.deltaCount()),
                csv(row.deltaProbability()),
                csv(row.observedExpectedRatio()),
                csv(row.direction())
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
