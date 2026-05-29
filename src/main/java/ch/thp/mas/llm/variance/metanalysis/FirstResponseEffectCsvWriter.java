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
public class FirstResponseEffectCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String HEADER = String.join(",",
            "series_id",
            "provider",
            "model",
            "model_family",
            "archetype",
            "prompt_language",
            "setting",
            "reasoning",
            "n_requested",
            "n_entries",
            "n_success",
            "n_failed",
            "literal_unique_count",
            "classification",
            "first_response_count",
            "dominant_response_count",
            "rest_unique_count",
            "first_ttft_seconds",
            "rest_ttft_seconds_p10",
            "rest_ttft_seconds_median",
            "rest_ttft_seconds_p90",
            "first_response_sha256",
            "dominant_response_sha256",
            "variant_summary"
    );

    public Path write(List<FirstResponseEffectRow> rows, String selection, String output) {
        Path target = outputPath(selection, output);
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
            throw new MetaAnalysisException("Could not write first response effect metanalysis CSV: " + target, e);
        }
    }

    private Path outputPath(String selection, String output) {
        if (output != null && !output.isBlank()) {
            return Path.of(output.trim()).normalize();
        }
        return DEFAULT_OUTPUT_DIRECTORY.resolve(defaultFilename(selection)).normalize();
    }

    private String defaultFilename(String selection) {
        String normalized = selection == null || selection.isBlank() ? "analysis" : selection.trim().replace('\\', '/');
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.equals("analysis") || name.isBlank()) {
            name = "all";
        }
        if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        return "1005-" + name + "-first-response-effect.csv";
    }

    private String csvLine(FirstResponseEffectRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelFamily()),
                csv(row.archetype()),
                csv(row.promptLanguage()),
                csv(row.setting()),
                csv(row.reasoning()),
                csv(row.nRequested()),
                csv(row.nEntries()),
                csv(row.nSuccess()),
                csv(row.nFailed()),
                csv(row.literalUniqueCount()),
                csv(row.classification()),
                csv(row.firstResponseCount()),
                csv(row.dominantResponseCount()),
                csv(row.restUniqueCount()),
                csv(row.firstTtftSeconds()),
                csv(row.restTtftSecondsP10()),
                csv(row.restTtftSecondsMedian()),
                csv(row.restTtftSecondsP90()),
                csv(row.firstResponseSha256()),
                csv(row.dominantResponseSha256()),
                csv(row.variantSummary())
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
