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
public class BaselineScatterCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String HEADER = String.join(",",
            "series_id",
            "provider",
            "model",
            "model_family",
            "model_family_id",
            "archetype",
            "archetype_id",
            "prompt_language",
            "setting",
            "n_requested",
            "n_success",
            "literal_unique_count",
            "literal_unique_share",
            "semantic_valid_rate",
            "plot_literal_unique_count",
            "plot_semantic_valid_rate"
    );

    public Path write(List<BaselineScatterRow> rows, String selection, String output) {
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
            throw new MetaAnalysisException("Could not write baseline scatter metanalysis CSV: " + target, e);
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
        return "1002-" + name + "-baseline-scatter.csv";
    }

    private String csvLine(BaselineScatterRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelFamily()),
                csv(row.modelFamilyId()),
                csv(row.archetype()),
                csv(row.archetypeId()),
                csv(row.promptLanguage()),
                csv(row.setting()),
                csv(row.nRequested()),
                csv(row.nSuccess()),
                csv(row.literalUniqueCount()),
                csv(row.literalUniqueShare()),
                csv(row.semanticValidRate()),
                csv(row.plotLiteralUniqueCount()),
                csv(row.plotSemanticValidRate())
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
