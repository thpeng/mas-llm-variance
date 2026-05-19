package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidAnalysis;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesTimestampedAnalysisJson() throws Exception {
        AnalysisWriter writer = new AnalysisWriter(tempDir, new AnalysisFileNameFactory(), objectMapper());

        Path written = writer.write("run.json", result());

        assertThat(written.getFileName().toString()).isEqualTo("run-analyze-20260502-110000-000.json");
        assertThat(Files.readString(written)).contains("\"sourceRun\":\"run.json\"");
    }

    @Test
    void refusesToOverwriteExistingAnalysis() {
        AnalysisWriter writer = new AnalysisWriter(tempDir, new AnalysisFileNameFactory(), objectMapper());
        writer.write("run.json", result());

        assertThatThrownBy(() -> writer.write("run.json", result()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("Could not write analysis");
    }

    private static AnalysisResult result() {
        return new AnalysisResult(
                "run.json",
                OffsetDateTime.parse("2026-05-02T11:00:00+02:00"),
                AnalysisConfig.defaults(),
                new AnalysisRunInfo("0001-test", InferenceProvider.OPENAI, "gpt-test", null, 1, 0.0, null, null, null, Reasoning.OFF),
                List.of(new AnalysisScan(
                        ClusteringAlgorithm.HIERARCHICAL,
                        "threshold",
                        0.08,
                        0,
                        new SemanticAnalysis(
                                1,
                                0,
                                new MedoidAnalysis(1, 0.0, "Bern"),
                                new MetricSummary(0, null, null, null, null, null),
                                List.of(),
                                List.of()
                        ),
                        new SyntacticAnalysis(List.of())
                )),
                null,
                null,
                null,
                null,
                new LiteralAnalysis(true, 1, 1, 1.0, List.of())
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
