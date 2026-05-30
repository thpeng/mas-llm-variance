package ch.thp.mas.llm.variance.metanalysis;

import java.util.List;
import java.util.Map;

public record ManualEvaluationSample(
        String schema,
        String sampleId,
        long seed,
        String instructions,
        Map<String, List<String>> allowedValues,
        int itemCount,
        List<ManualEvaluationSampleItem> items
) {
}
