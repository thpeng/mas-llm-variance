package ch.thp.mas.llm.variance.metanalysis;

public record ManualRoundTripEvaluationFields(
        String halluzination,
        String kommentar
) {

    public static ManualRoundTripEvaluationFields empty() {
        return new ManualRoundTripEvaluationFields(null, "");
    }
}
