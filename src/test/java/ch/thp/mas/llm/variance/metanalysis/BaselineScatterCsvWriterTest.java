package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineScatterCsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesScatterCsv() throws Exception {
        Path output = tempDir.resolve("scatter.csv");
        BaselineScatterCsvWriter writer = new BaselineScatterCsvWriter();
        BaselineScatterRow row = new BaselineScatterRow(
                "0000-openai-gpt4o-20240513-roundtrip-de-baseline",
                InferenceProvider.OPENAI,
                "gpt-4o-2024-05-13",
                "gpt-4o",
                1,
                PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP,
                1,
                "DE",
                "baseline",
                100,
                99,
                5,
                5.0 / 99.0,
                0.97,
                4.34,
                0.9535
        );

        writer.write(List.of(row), "analysis/main_100_iterations", output.toString());

        String csv = Files.readString(output);
        assertThat(csv).startsWith("series_id,provider,model,model_family,model_family_id,archetype,archetype_id");
        assertThat(csv).contains("0000-openai-gpt4o-20240513-roundtrip-de-baseline,OPENAI,gpt-4o-2024-05-13,gpt-4o,1");
        assertThat(csv).contains(",100,99,5,0.050505050505050504,0.97,4.34,0.9535");
    }
}
