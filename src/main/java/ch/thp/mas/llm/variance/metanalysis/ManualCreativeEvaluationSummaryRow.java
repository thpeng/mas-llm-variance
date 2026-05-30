package ch.thp.mas.llm.variance.metanalysis;

public record ManualCreativeEvaluationSummaryRow(
        String seriesId,
        String model,
        String setting,
        int sampleSize,
        int sampleLiteralUniqueCount,
        int analysisLucerneFoundCount,
        int analysisThreeSentencesCount,
        int manualTourismReferenceCount,
        int manualPlaceReferenceCount,
        int manualHallucinationCount
) {
}
