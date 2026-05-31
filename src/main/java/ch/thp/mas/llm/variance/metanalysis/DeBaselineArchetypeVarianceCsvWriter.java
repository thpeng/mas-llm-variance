package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DeBaselineArchetypeVarianceCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String DEFAULT_FILENAME = "1009-main-de-baseline-archetype-variance-summary.csv";
    private static final String HEADER = String.join(",",
            "archetype",
            "archetype_label",
            "prompt_language",
            "setting",
            "series_count",
            "mean_literal_unique_count",
            "median_literal_unique_count",
            "max_literal_unique_count"
    );

    public Path write(List<DeBaselineArchetypeVarianceRow> rows, String output) {
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
            throw new MetaAnalysisException("Could not write DE baseline archetype variance CSV: " + target, e);
        }
    }

    private String csvLine(DeBaselineArchetypeVarianceRow row) {
        return String.join(",",
                csv(row.archetype()),
                csv(row.archetypeLabel()),
                csv(row.promptLanguage()),
                csv(row.setting()),
                csv(row.seriesCount()),
                csv(format(row.meanLiteralUniqueCount())),
                csv(format(row.medianLiteralUniqueCount())),
                csv(row.maxLiteralUniqueCount())
        );
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
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
