package ch.thp.mas.llm.variance.metanalysis;

import java.util.List;

public record ManualEvaluationSampleItem(
        String id,
        int sampleNumber,
        List<String> responseLines,
        Object evaluation
) {
}
