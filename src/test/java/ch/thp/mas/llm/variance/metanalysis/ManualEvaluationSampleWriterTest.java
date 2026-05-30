package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ManualEvaluationSampleWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesManualEvaluationSampleJsonFiles() throws Exception {
        ManualEvaluationSampleWriter writer = new ManualEvaluationSampleWriter();
        Path output = tempDir.resolve("manual-review");
        ManualEvaluationSample roundTrip = new ManualEvaluationSample(
                "schema",
                "roundtrip",
                123L,
                "instructions",
                Map.of("halluzination", List.of("Nein", "Ja", "nicht bestimmbar")),
                1,
                List.of(new ManualEvaluationSampleItem(
                        "bms-abc",
                        1,
                        List.of("erste Zeile", "zweite Zeile"),
                        ManualRoundTripEvaluationFields.empty()
                ))
        );
        ManualEvaluationSample creative = new ManualEvaluationSample(
                "schema",
                "creative",
                123L,
                "instructions",
                Map.of(
                        "halluzination", List.of("Nein", "Ja", "nicht bestimmbar"),
                        "tourismusbezug", List.of("true", "false"),
                        "luzernbezug", List.of("true", "false")
                ),
                1,
                List.of(new ManualEvaluationSampleItem(
                        "bms-def",
                        1,
                        List.of("Luzern ist schoen."),
                        ManualCreativeEvaluationFields.empty()
                ))
        );

        writer.write(new ManualEvaluationSampleExport(roundTrip, creative), output.toString());

        String roundTripJson = Files.readString(output.resolve("1007-main-manual-evaluation-roundtrip-sample.json"));
        String creativeJson = Files.readString(output.resolve("1007-main-manual-evaluation-creative-sample.json"));
        assertThat(roundTripJson).contains("\"id\" : \"bms-abc\"");
        assertThat(roundTripJson).doesNotContain("tourismusbezug");
        assertThat(roundTripJson).contains("\"halluzination\" : null");
        assertThat(roundTripJson).contains("\"responseLines\"");
        assertThat(creativeJson).contains("\"id\" : \"bms-def\"");
        assertThat(creativeJson).contains("\"tourismusbezug\" : null");
        assertThat(creativeJson).contains("\"luzernbezug\" : null");
    }
}
