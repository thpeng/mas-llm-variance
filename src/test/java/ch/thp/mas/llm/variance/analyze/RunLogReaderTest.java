package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunLogReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsRunLogByFilenameWithOptionalSuffix() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        Files.writeString(tempDir.resolve("run.json"), objectMapper.writeValueAsString(runLog()));
        RunLogReader reader = new RunLogReader(tempDir, objectMapper);

        NamedRunLog namedRunLog = reader.read("run");

        assertThat(namedRunLog.filename()).isEqualTo("run.json");
        assertThat(namedRunLog.runLog().planName()).isEqualTo("0001-test");
    }

    @Test
    void readAllReturnsRunLogsInSortedOrder() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        Files.writeString(tempDir.resolve("b.json"), objectMapper.writeValueAsString(runLog()));
        Files.writeString(tempDir.resolve("a.json"), objectMapper.writeValueAsString(runLog()));
        Files.writeString(tempDir.resolve("ignore.txt"), "not json");
        RunLogReader reader = new RunLogReader(tempDir, objectMapper);

        List<NamedRunLog> runLogs = reader.readAll();

        assertThat(runLogs).extracting(NamedRunLog::filename).containsExactly("a.json", "b.json");
    }

    @Test
    void rejectsPathTraversal() {
        RunLogReader reader = new RunLogReader(tempDir, objectMapper());

        assertThatThrownBy(() -> reader.read("../run"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("Invalid run log filename");
    }

    private static RunLog runLog() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new RunLog(
                "0001-test",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                1,
                new RunConfigLog(0.0, null, null, null, "off"),
                "prompt",
                List.of(new RunLogEntry(1, now, now, "Bern", null))
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
