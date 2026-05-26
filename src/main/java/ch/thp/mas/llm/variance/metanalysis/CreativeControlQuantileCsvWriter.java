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
public class CreativeControlQuantileCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String HEADER = String.join(",",
            "series_id",
            "provider",
            "model",
            "model_version",
            "model_family",
            "archetype",
            "prompt_language",
            "setting",
            "setting_label",
            "plot_order",
            "temperature",
            "top_p",
            "top_k",
            "reasoning",
            "n_requested",
            "n_semantic_success",
            "literal_unique_count",
            "literal_top1_share",
            "literal_variance_share",
            "semantic_valid_rate",
            "pair_count",
            "p10_rouge_distance",
            "median_rouge_distance",
            "p90_rouge_distance",
            "p10_bleu_distance",
            "median_bleu_distance",
            "p90_bleu_distance"
    );

    public Path write(List<CreativeControlQuantileRow> rows, String selection, String output) {
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
            throw new MetaAnalysisException("Could not write creative control quantile metanalysis CSV: " + target, e);
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
        return "1003-" + name + "-creative-control-quantiles.csv";
    }

    private String csvLine(CreativeControlQuantileRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelVersion()),
                csv(row.modelFamily()),
                csv(row.archetype()),
                csv(row.promptLanguage()),
                csv(row.setting()),
                csv(row.settingLabel()),
                csv(row.plotOrder()),
                csv(row.temperature()),
                csv(row.topP()),
                csv(row.topK()),
                csv(row.reasoning()),
                csv(row.nRequested()),
                csv(row.nSuccess()),
                csv(row.literalUniqueCount()),
                csv(row.literalTop1Share()),
                csv(1.0 - row.literalTop1Share()),
                csv(row.semanticValidRate()),
                csv(row.pairCount()),
                csv(row.p10RougeDistance()),
                csv(row.medianRougeDistance()),
                csv(row.p90RougeDistance()),
                csv(row.p10BleuDistance()),
                csv(row.medianBleuDistance()),
                csv(row.p90BleuDistance())
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
