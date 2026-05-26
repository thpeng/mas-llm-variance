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
public class WordDriftCsvWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("src", "main", "resources", "metanalysis");
    private static final String HEADER = String.join(",",
            "series_id",
            "provider",
            "model",
            "model_version",
            "n_success",
            "literal_distinct_response_count",
            "mean_cluster_size",
            "mean_rouge_distance",
            "mean_bleu_distance",
            "distinct_word_count",
            "mean_words_per_response",
            "p10_words_per_response",
            "p90_words_per_response",
            "distinct_words"
    );

    private final Path outputDirectory;

    public WordDriftCsvWriter() {
        this(DEFAULT_OUTPUT_DIRECTORY);
    }

    WordDriftCsvWriter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Path write(List<WordDriftRow> rows, String selection, String output) {
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
            throw new MetaAnalysisException("Could not write word drift metanalysis CSV: " + target, e);
        }
    }

    private Path outputPath(String selection, String output) {
        if (output != null && !output.isBlank()) {
            return Path.of(output.trim()).normalize();
        }
        return outputDirectory.resolve(defaultFilename(selection)).normalize();
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
        return "1001-" + name + "-word-analysis.csv";
    }

    private String csvLine(WordDriftRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.provider()),
                csv(row.model()),
                csv(row.modelVersion()),
                csv(row.nSuccess()),
                csv(row.literalDistinctResponseCount()),
                csv(row.meanClusterSize()),
                csv(row.meanRougeDistance()),
                csv(row.meanBleuDistance()),
                csv(row.distinctWordCount()),
                csv(row.meanWordsPerResponse()),
                csv(row.p10WordsPerResponse()),
                csv(row.p90WordsPerResponse()),
                csv(row.distinctWords())
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
