package ch.thp.mas.llm.variance.metanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class ManualEvaluationSampleWriter {

    private static final Path DEFAULT_OUTPUT = Path.of(
            "src",
            "main",
            "resources",
            "analysis",
            "manual_review"
    );

    private final ObjectMapper objectMapper;

    public ManualEvaluationSampleWriter() {
        this(new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    ManualEvaluationSampleWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path write(ManualEvaluationSampleExport export, String output) {
        Path targetDirectory = output == null || output.isBlank() ? DEFAULT_OUTPUT : Path.of(output.trim()).normalize();
        try {
            Files.createDirectories(targetDirectory);
            writeOne(targetDirectory.resolve("1007-main-manual-evaluation-creative-sample.json"), export.creative());
            return targetDirectory;
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not write manual evaluation sample JSON files: " + targetDirectory, e);
        }
    }

    private void writeOne(Path target, ManualEvaluationSample sample) throws Exception {
        String json = objectMapper.writeValueAsString(sample) + System.lineSeparator();
        Files.writeString(
                target,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}
