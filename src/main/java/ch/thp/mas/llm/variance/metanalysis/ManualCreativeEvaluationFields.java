package ch.thp.mas.llm.variance.metanalysis;

public record ManualCreativeEvaluationFields(
        Boolean tourismusbezug,
        Boolean luzernbezug,
        String halluzination,
        String kommentar
) {

    public static ManualCreativeEvaluationFields empty() {
        return new ManualCreativeEvaluationFields(null, null, null, "");
    }
}
