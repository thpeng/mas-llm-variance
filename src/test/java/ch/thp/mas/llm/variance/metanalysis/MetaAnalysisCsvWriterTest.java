package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetaAnalysisCsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesHeaderAndEscapedValues() throws Exception {
        Path output = tempDir.resolve("summary.csv");
        MetaAnalysisCsvWriter writer = new MetaAnalysisCsvWriter();
        MetaAnalysisRow row = new MetaAnalysisRow(
                "0001-test",
                InferenceProvider.OPENAI,
                "model,with-comma",
                "model-version",
                PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING,
                "DE",
                "baseline",
                0.0,
                1.0,
                1,
                "1",
                "off",
                1,
                1,
                0,
                1.0,
                0.0,
                1,
                1.0,
                1.0,
                0.1,
                0.2,
                0.3,
                0.4,
                10,
                9.0,
                10.0,
                11.0,
                20,
                18.0,
                20.0,
                22.0,
                0,
                0.0,
                0.0,
                0.0,
                1.25,
                1.0,
                1.25,
                1.5
        );

        writer.write(List.of(row), "analysis/test", output.toString());

        String csv = Files.readString(output);
        assertThat(csv).startsWith("series_id,provider,model,model_version,archetype");
        assertThat(csv).contains("0001-test,OPENAI,\"model,with-comma\"");
        assertThat(csv).contains("10,9.0,10.0,11.0,20,18.0,20.0,22.0,0,0.0,0.0,0.0,1.25,1.0,1.25,1.5");
    }
}
