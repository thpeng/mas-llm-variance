package ch.thp.mas.llm.variance.run;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class RunFileNameFactory {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public String create(OffsetDateTime timestamp, String planName) {
        return FORMATTER.format(timestamp) + "-run-" + sanitize(planName) + ".json";
    }

    private String sanitize(String planName) {
        return planName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
