package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MetaAnalysisCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String HEADER = String.join(",",
            "series_id",
            "provider",
            "model",
            "model_version",
            "archetype",
            "prompt_language",
            "setting",
            "temperature",
            "top_p",
            "top_k",
            "seed",
            "reasoning",
            "n_requested",
            "n_success",
            "n_failed",
            "semantic_valid_rate",
            "semantic_outlier_rate",
            "literal_unique_count",
            "literal_top1_share",
            "largest_cluster_share",
            "median_rouge_distance",
            "p90_rouge_distance",
            "median_bleu_distance",
            "p90_bleu_distance",
            "input_tokens_total",
            "input_tokens_p10",
            "input_tokens_median",
            "input_tokens_p90",
            "output_tokens_total",
            "output_tokens_p10",
            "output_tokens_median",
            "output_tokens_p90",
            "reasoning_tokens_total",
            "reasoning_tokens_p10",
            "reasoning_tokens_median",
            "reasoning_tokens_p90",
            "duration_seconds_total",
            "duration_seconds_p10",
            "duration_seconds_median",
            "duration_seconds_p90"
    );

    public Path write(List<MetaAnalysisRow> rows, String selection, String output) {
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
            throw new MetaAnalysisException("Could not write metanalysis CSV: " + target, e);
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
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        return name + ".csv";
    }

    private String csvLine(MetaAnalysisRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelVersion()),
                csv(row.archetype()),
                csv(row.promptLanguage()),
                csv(row.setting()),
                csv(row.temperature()),
                csv(row.topP()),
                csv(row.topK()),
                csv(row.seed()),
                csv(row.reasoning()),
                csv(row.nRequested()),
                csv(row.nSuccess()),
                csv(row.nFailed()),
                csv(row.semanticValidRate()),
                csv(row.semanticOutlierRate()),
                csv(row.literalUniqueCount()),
                csv(row.literalTop1Share()),
                csv(row.largestClusterShare()),
                csv(row.medianRougeDistance()),
                csv(row.p90RougeDistance()),
                csv(row.medianBleuDistance()),
                csv(row.p90BleuDistance()),
                csv(row.inputTokensTotal()),
                csv(row.inputTokensP10()),
                csv(row.inputTokensMedian()),
                csv(row.inputTokensP90()),
                csv(row.outputTokensTotal()),
                csv(row.outputTokensP10()),
                csv(row.outputTokensMedian()),
                csv(row.outputTokensP90()),
                csv(row.reasoningTokensTotal()),
                csv(row.reasoningTokensP10()),
                csv(row.reasoningTokensMedian()),
                csv(row.reasoningTokensP90()),
                csv(row.durationSecondsTotal()),
                csv(row.durationSecondsP10()),
                csv(row.durationSecondsMedian()),
                csv(row.durationSecondsP90())
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
