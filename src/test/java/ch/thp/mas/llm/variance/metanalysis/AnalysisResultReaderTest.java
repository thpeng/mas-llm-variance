package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
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

class AnalysisResultReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAnalysisResultByFilenameWithOptionalSuffix() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        Files.writeString(tempDir.resolve("analysis.json"), objectMapper.writeValueAsString(analysisResult()));
        AnalysisResultReader reader = new AnalysisResultReader(tempDir, objectMapper);

        List<NamedAnalysisResult> results = reader.readSelection("analysis");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().filename()).isEqualTo("analysis.json");
        assertThat(results.getFirst().analysisResult().run().planName()).isEqualTo("0001-test");
    }

    @Test
    void readSelectionReturnsAnalysisResultsInSubfolder() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        Files.createDirectories(tempDir.resolve("main/gpt"));
        Files.createDirectories(tempDir.resolve("smoke"));
        Files.writeString(tempDir.resolve("main/gpt/b.json"), objectMapper.writeValueAsString(analysisResult()));
        Files.writeString(tempDir.resolve("main/gpt/a.json"), objectMapper.writeValueAsString(analysisResult()));
        Files.writeString(tempDir.resolve("smoke/c.json"), objectMapper.writeValueAsString(analysisResult()));
        AnalysisResultReader reader = new AnalysisResultReader(tempDir, objectMapper);

        List<NamedAnalysisResult> results = reader.readSelection("analysis/main");

        assertThat(results).extracting(NamedAnalysisResult::filename)
                .containsExactly("main/gpt/a.json", "main/gpt/b.json");
    }

    @Test
    void rejectsAmbiguousAnalysisFileSelection() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        Files.createDirectories(tempDir.resolve("gpt"));
        Files.createDirectories(tempDir.resolve("google"));
        Files.writeString(tempDir.resolve("gpt/result.json"), objectMapper.writeValueAsString(analysisResult()));
        Files.writeString(tempDir.resolve("google/result.json"), objectMapper.writeValueAsString(analysisResult()));
        AnalysisResultReader reader = new AnalysisResultReader(tempDir, objectMapper);

        assertThatThrownBy(() -> reader.readSelection("result"))
                .isInstanceOf(MetaAnalysisException.class)
                .hasMessageContaining("Ambiguous analysis selection");
    }

    @Test
    void rejectsPathTraversal() {
        AnalysisResultReader reader = new AnalysisResultReader(tempDir, objectMapper());

        assertThatThrownBy(() -> reader.readSelection("../analysis"))
                .isInstanceOf(MetaAnalysisException.class)
                .hasMessageContaining("Invalid analysis selection");
    }

    private static AnalysisResult analysisResult() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new AnalysisResult(
                "main/run.json",
                now,
                AnalysisConfig.defaults(),
                new AnalysisRunInfo(
                        "0001-test",
                        InferenceProvider.OPENAI,
                        "gpt-test",
                        "gpt-test-version",
                        1,
                        0.0,
                        1.0,
                        null,
                        null,
                        Reasoning.OFF
                ),
                null,
                null,
                null,
                null,
                new LiteralAnalysis(true, 1, 1, 1.0)
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
