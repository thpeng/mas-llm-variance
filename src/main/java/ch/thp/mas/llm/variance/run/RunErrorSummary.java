package ch.thp.mas.llm.variance.run;

import java.util.List;

public record RunErrorSummary(int servingErrorCount) {

    public static RunErrorSummary from(List<RunLogEntry> repetitions) {
        int servingErrors = (int) repetitions.stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SERVING_ERROR)
                .count();
        return new RunErrorSummary(servingErrors);
    }
}
