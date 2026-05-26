package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextExtraction;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextStatus;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CreativeControlQuantileExporter {

    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;

    public CreativeControlQuantileExporter(RougeLMetric rougeLMetric, BleuMetric bleuMetric) {
        this.rougeLMetric = rougeLMetric;
        this.bleuMetric = bleuMetric;
    }

    public List<CreativeControlQuantileRow> exportRows(List<NamedAnalysisResult> analyses) {
        return analyses.stream()
                .filter(this::isCreativeControlSeries)
                .map(this::exportRow)
                .sorted(Comparator.comparingInt(CreativeControlQuantileRow::plotOrder).reversed())
                .toList();
    }

    private boolean isCreativeControlSeries(NamedAnalysisResult namedAnalysis) {
        AnalysisResult analysis = namedAnalysis.analysisResult();
        return analysis.lucerneMarketingText() != null
                && analysis.config().promptEvaluation() == PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING
                && isTargetModel(analysis.run().model())
                && List.of("baseline", "mittel", "hoch").contains(setting(analysis.run().planName()));
    }

    private CreativeControlQuantileRow exportRow(NamedAnalysisResult namedAnalysis) {
        AnalysisResult analysis = namedAnalysis.analysisResult();
        List<String> responses = successfulResponses(analysis);
        List<Double> rougeDistances = new ArrayList<>();
        List<Double> bleuDistances = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            for (int j = i + 1; j < responses.size(); j++) {
                String left = responses.get(i);
                String right = responses.get(j);
                rougeDistances.add(1.0 - rougeLMetric.score(left, right));
                bleuDistances.add(1.0 - bleuMetric.score(left, right, analysis.config().bleu()));
            }
        }

        Quantiles rouge = quantiles(rougeDistances);
        Quantiles bleu = quantiles(bleuDistances);
        String setting = setting(analysis.run().planName());
        String modelFamily = modelFamily(analysis.run().model());
        return new CreativeControlQuantileRow(
                analysis.run().planName(),
                analysis.run().inferenceProvider(),
                analysis.run().model(),
                analysis.run().modelVersion(),
                modelFamily,
                analysis.config().promptEvaluation(),
                "DE",
                setting,
                settingLabel(setting),
                plotOrder(modelFamily, setting),
                analysis.run().temperature(),
                analysis.run().topP(),
                analysis.run().topK(),
                analysis.run().reasoning() == null ? null : analysis.run().reasoning().name().toLowerCase(Locale.ROOT),
                analysis.run().iterations(),
                responses.size(),
                analysis.literal() == null ? 0 : analysis.literal().distinctResponseCount(),
                top1Share(responses),
                analysis.lucerneMarketingText().successShare(),
                rougeDistances.size(),
                rouge.p10(),
                rouge.median(),
                rouge.p90(),
                bleu.p10(),
                bleu.median(),
                bleu.p90()
        );
    }

    private List<String> successfulResponses(AnalysisResult analysis) {
        return analysis.lucerneMarketingText().extractions().stream()
                .filter(extraction -> extraction.status() == LucerneMarketingTextStatus.SUCCESS)
                .map(LucerneMarketingTextExtraction::rawResponse)
                .toList();
    }

    private double top1Share(List<String> responses) {
        if (responses.isEmpty()) {
            return 0.0;
        }
        int topCount = responses.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream()
                .mapToInt(Long::intValue)
                .max()
                .orElse(0);
        return (double) topCount / responses.size();
    }

    private Quantiles quantiles(List<Double> values) {
        if (values.isEmpty()) {
            return new Quantiles(null, null, null);
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return new Quantiles(
                nearestRank(sorted, 0.1),
                median(sorted),
                nearestRank(sorted, 0.9)
        );
    }

    private Double median(List<Double> sorted) {
        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private Double nearestRank(List<Double> sorted, double percentile) {
        int rank = (int) Math.ceil(percentile * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }

    private boolean isTargetModel(String model) {
        String family = modelFamily(model);
        return "gpt-5.4-mini".equals(family) || "apertus".equals(family);
    }

    private String modelFamily(String model) {
        if (model == null) {
            return "";
        }
        String normalized = model.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("gpt-5.4-mini") || normalized.startsWith("gpt54mini")) {
            return "gpt-5.4-mini";
        }
        if (normalized.contains("apertus")) {
            return "apertus";
        }
        return model;
    }

    private String setting(String planName) {
        if (planName == null) {
            return "";
        }
        String normalized = planName.toLowerCase(Locale.ROOT);
        if (normalized.contains("-hoch")) {
            return "hoch";
        }
        if (normalized.contains("-mittel")) {
            return "mittel";
        }
        return "baseline";
    }

    private String settingLabel(String setting) {
        return switch (setting) {
            case "baseline" -> "Basis";
            case "mittel" -> "Mittel";
            case "hoch" -> "Hoch";
            default -> setting;
        };
    }

    private int plotOrder(String modelFamily, String setting) {
        Map<String, Integer> base = Map.of(
                "gpt-5.4-mini", 6,
                "apertus", 3
        );
        int settingOffset = switch (setting) {
            case "baseline" -> 0;
            case "mittel" -> -1;
            case "hoch" -> -2;
            default -> 0;
        };
        return base.getOrDefault(modelFamily, 0) + settingOffset;
    }

    private record Quantiles(Double p10, Double median, Double p90) {
    }
}
