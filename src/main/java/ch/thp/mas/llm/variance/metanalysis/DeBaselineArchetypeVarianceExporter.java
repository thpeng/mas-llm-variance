package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DeBaselineArchetypeVarianceExporter {

    public List<DeBaselineArchetypeVarianceRow> exportRows(List<MetaAnalysisRow> rows) {
        Map<PromptEvaluation, List<MetaAnalysisRow>> rowsByArchetype = new EnumMap<>(PromptEvaluation.class);
        rows.stream()
                .filter(row -> "DE".equals(row.promptLanguage()))
                .filter(row -> "baseline".equals(row.setting()))
                .filter(this::isIncludedModelConfiguration)
                .forEach(row -> rowsByArchetype
                        .computeIfAbsent(row.archetype(), ignored -> new ArrayList<>())
                        .add(row));

        return rowsByArchetype.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> archetypeOrder(entry.getKey())))
                .map(entry -> exportRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private DeBaselineArchetypeVarianceRow exportRow(PromptEvaluation archetype, List<MetaAnalysisRow> rows) {
        int seriesCount = rows.size();
        return new DeBaselineArchetypeVarianceRow(
                archetype,
                archetypeLabel(archetype),
                "DE",
                "baseline",
                seriesCount,
                average(rows.stream().map(row -> (double) row.literalUniqueCount()).toList()),
                median(rows.stream().map(row -> (double) row.literalUniqueCount()).toList()),
                rows.stream().mapToInt(MetaAnalysisRow::literalUniqueCount).max().orElse(0)
        );
    }

    private boolean isIncludedModelConfiguration(MetaAnalysisRow row) {
        String model = row.model() == null ? "" : row.model().toLowerCase(java.util.Locale.ROOT);
        if (model.startsWith("gpt-4o")) {
            return false;
        }
        if ("openai/gpt-oss-20b".equals(model)) {
            return "low".equals(row.reasoning());
        }
        return true;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private int archetypeOrder(PromptEvaluation archetype) {
        return switch (archetype) {
            case CREATIVE_GENERATIVE_LUCERNE_MARKETING -> 1;
            case ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP -> 2;
            case FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION -> 3;
            case LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE -> 4;
        };
    }

    private String archetypeLabel(PromptEvaluation archetype) {
        return switch (archetype) {
            case CREATIVE_GENERATIVE_LUCERNE_MARKETING -> "Kreativ-generativ";
            case ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP -> "Beratend-empfehlend";
            case FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION -> "Faktisch-kritisch";
            case LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE -> "Literal-formatkritisch";
        };
    }
}
