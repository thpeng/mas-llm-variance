package ch.thp.mas.llm.variance.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnalysisWriter {

    private static final Path DEFAULT_ANALYSIS_DIRECTORY = Path.of("src", "main", "resources", "analysis");

    private final Path analysisDirectory;
    private final AnalysisFileNameFactory fileNameFactory;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalysisWriter(AnalysisFileNameFactory fileNameFactory) {
        this(DEFAULT_ANALYSIS_DIRECTORY, fileNameFactory, defaultObjectMapper());
    }

    AnalysisWriter(Path analysisDirectory, AnalysisFileNameFactory fileNameFactory, ObjectMapper objectMapper) {
        this.analysisDirectory = analysisDirectory;
        this.fileNameFactory = fileNameFactory;
        this.objectMapper = objectMapper;
    }

    public Path write(String sourceRunFilename, AnalysisResult result) {
        try {
            Files.createDirectories(analysisDirectory);
            Path target = analysisDirectory.resolve(fileNameFactory.create(sourceRunFilename, result.analyzedAt()));
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return Files.writeString(
                    target,
                    objectMapper.writeValueAsString(result),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new AnalysisException("Could not write analysis for run: " + sourceRunFilename, e);
        }
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
