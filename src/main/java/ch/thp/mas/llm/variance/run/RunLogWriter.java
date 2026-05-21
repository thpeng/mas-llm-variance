package ch.thp.mas.llm.variance.run;

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
public class RunLogWriter {

    private static final Path DEFAULT_RUNS_DIRECTORY = Path.of("src", "main", "resources", "runs");

    private final Path runsDirectory;
    private final RunFileNameFactory fileNameFactory;
    private final ObjectMapper objectMapper;

    @Autowired
    public RunLogWriter(RunFileNameFactory fileNameFactory) {
        this(DEFAULT_RUNS_DIRECTORY, fileNameFactory, defaultObjectMapper());
    }

    RunLogWriter(Path runsDirectory, RunFileNameFactory fileNameFactory, ObjectMapper objectMapper) {
        this.runsDirectory = runsDirectory;
        this.fileNameFactory = fileNameFactory;
        this.objectMapper = objectMapper;
    }

    public Path write(RunLog runLog) {
        return write(runLog, "");
    }

    public Path write(RunLog runLog, String sourcePlanPath) {
        try {
            Files.createDirectories(runsDirectory);
            Path targetDirectory = runsDirectory.resolve(relativeDirectory(sourcePlanPath)).normalize();
            ensureInsideRunsDirectory(targetDirectory);
            Files.createDirectories(targetDirectory);
            Path target = targetDirectory.resolve(fileNameFactory.create(runLog.startedAt(), runLog.planName()));
            String json = objectMapper.writeValueAsString(runLog);
            return Files.writeString(
                    target,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new RunLoggingException("Could not write run log for plan: " + runLog.planName(), e);
        }
    }

    private Path relativeDirectory(String sourcePlanPath) {
        if (sourcePlanPath == null || sourcePlanPath.isBlank()) {
            return Path.of("");
        }
        Path parent = Path.of(sourcePlanPath.replace('\\', '/')).getParent();
        return parent == null ? Path.of("") : parent;
    }

    private void ensureInsideRunsDirectory(Path targetDirectory) {
        if (!targetDirectory.startsWith(runsDirectory.normalize())) {
            throw new IllegalArgumentException("Run log target must stay under runs directory: " + targetDirectory);
        }
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
