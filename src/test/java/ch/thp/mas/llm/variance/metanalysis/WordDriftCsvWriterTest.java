package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WordDriftCsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesWordDriftCsvAndUses1001DefaultFilename() throws Exception {
        WordDriftCsvWriter writer = new WordDriftCsvWriter(tempDir);
        Path output = writer.write(
                List.of(new WordDriftRow(
                        "0001-test",
                        InferenceProvider.OPENAI,
                        "gpt-4o-test",
                        "gpt-4o-test",
                        2,
                        2,
                        1.5,
                        0.25,
                        0.5,
                        3,
                        4.5,
                        4,
                        5,
                        "bern luzern zurich"
                )),
                tempDir.resolve("analysis/main").toString(),
                null
        );

        assertThat(output.getFileName().toString()).isEqualTo("1001-main-word-analysis.csv");
        String csv = Files.readString(output);
        assertThat(csv).startsWith("series_id,provider,model,model_version,n_success");
        assertThat(csv).contains(
                "0001-test,OPENAI,gpt-4o-test,gpt-4o-test,2,2,1.5,0.25,0.5,3,4.5,4,5,bern luzern zurich");
    }
}
