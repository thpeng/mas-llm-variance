package ch.thp.mas.llm.variance.analyze;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AnalysisFileNameFactory {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public String create(String runLogFilename, OffsetDateTime analyzedAt) {
        String base = runLogFilename.endsWith(".json")
                ? runLogFilename.substring(0, runLogFilename.length() - ".json".length())
                : runLogFilename;
        return base + "-analyze-" + FORMATTER.format(analyzedAt) + ".json";
    }
}
