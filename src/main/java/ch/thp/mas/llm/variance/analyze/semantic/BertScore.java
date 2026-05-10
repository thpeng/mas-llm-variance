package ch.thp.mas.llm.variance.analyze.semantic;

public record BertScore(double precision, double recall, double f1) {

    public BertScore {
        requireProbability("precision", precision);
        requireProbability("recall", recall);
        requireProbability("f1", f1);
    }

    private static void requireProbability(String name, double value) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be in [0.0, 1.0]");
        }
    }
}
