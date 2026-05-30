package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ManualCreativeEvaluationSummaryCsvWriter {

    private static final Path DEFAULT_OUTPUT = Path.of(
            "src",
            "main",
            "resources",
            "metanalysis",
            "1007-main-manual-creative-evaluation-summary.csv"
    );
    private static final String HEADER = String.join(",",
            "series_id",
            "model",
            "setting",
            "sample_size",
            "sample_literal_unique_count",
            "analysis_lucerne_found_count",
            "analysis_three_sentences_count",
            "manual_tourism_reference_count",
            "manual_place_reference_count",
            "manual_hallucination_count"
    );

    public Path write(List<ManualCreativeEvaluationSummaryRow> rows, String output) {
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
            throw new MetaAnalysisException("Could not write manual creative evaluation summary CSV: " + target, e);
        }
    }

    private String csvLine(ManualCreativeEvaluationSummaryRow row) {
        return String.join(",",
                csv(row.seriesId()),
                csv(row.model()),
                csv(row.setting()),
                csv(row.sampleSize()),
                csv(row.sampleLiteralUniqueCount()),
                csv(row.analysisLucerneFoundCount()),
                csv(row.analysisThreeSentencesCount()),
                csv(row.manualTourismReferenceCount()),
                csv(row.manualPlaceReferenceCount()),
                csv(row.manualHallucinationCount())
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
