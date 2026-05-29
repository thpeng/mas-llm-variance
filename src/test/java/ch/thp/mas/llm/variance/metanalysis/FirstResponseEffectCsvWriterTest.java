package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FirstResponseEffectCsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesFirstResponseEffectCsv() throws Exception {
        FirstResponseEffectCsvWriter writer = new FirstResponseEffectCsvWriter();
        Path output = tempDir.resolve("first-response.csv");

        writer.write(List.of(new FirstResponseEffectRow(
                "0300-lmstudio-apertus-roundtrip-de-baseline",
                InferenceProvider.LMSTUDIO,
                "swiss-ai_apertus-8b-instruct-2509",
                "apertus",
                PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP,
                "DE",
                "baseline",
                "off",
                100,
                100,
                100,
                0,
                2,
                FirstResponseEffectClassification.FIRST_DIFF_REST_SAME,
                1,
                99,
                1,
                0.1,
                0.2,
                0.3,
                0.4,
                "abc",
                "def",
                "v1=1@1#abc;v2=99@2-100#def"
        )), "analysis/main_100_iterations", output.toString());

        String csv = Files.readString(output);
        assertThat(csv).contains("classification,first_response_count,dominant_response_count");
        assertThat(csv).contains("first_ttft_seconds,rest_ttft_seconds_p10,rest_ttft_seconds_median,rest_ttft_seconds_p90");
        assertThat(csv).contains("FIRST_DIFF_REST_SAME");
        assertThat(csv).contains(",0.1,0.2,0.3,0.4,");
        assertThat(csv).contains("v1=1@1#abc;v2=99@2-100#def");
    }
}
