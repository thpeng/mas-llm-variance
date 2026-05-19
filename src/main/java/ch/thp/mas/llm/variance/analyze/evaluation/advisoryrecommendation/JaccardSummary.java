package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

public record JaccardSummary(
        int count,
        Double min,
        Double p10,
        Double median,
        Double p90,
        Double max,
        Double mean
) {
}
