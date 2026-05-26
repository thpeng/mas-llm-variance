package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CreativeControlQuantileCsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesRangePlotCsv() throws Exception {
        CreativeControlQuantileCsvWriter writer = new CreativeControlQuantileCsvWriter();
        Path output = tempDir.resolve("creative-control.csv");

        writer.write(List.of(new CreativeControlQuantileRow(
                "0020-openai-gpt54mini-creative-baseline",
                InferenceProvider.OPENAI,
                "gpt-5.4-mini-2026-03-17",
                "gpt-5.4-mini-2026-03-17",
                "gpt-5.4-mini",
                PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING,
                "DE",
                "baseline",
                "Basis",
                6,
                0.0,
                1.0,
                null,
                "off",
                100,
                100,
                45,
                0.11,
                1.0,
                4950,
                0.1,
                0.2,
                0.3,
                0.4,
                0.5,
                0.6
        )), "analysis/main_100_iterations", output.toString());

        String csv = Files.readString(output);
        assertThat(csv).contains("p10_bleu_distance,median_bleu_distance,p90_bleu_distance");
        assertThat(csv).contains("0020-openai-gpt54mini-creative-baseline");
        assertThat(csv).contains(",0.89,");
    }
}
