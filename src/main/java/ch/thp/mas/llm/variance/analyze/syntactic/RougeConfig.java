package ch.thp.mas.llm.variance.analyze.syntactic;

import java.util.Objects;

public record RougeConfig(Variant variant, Aggregation aggregation) {
    public enum Variant { ROUGE_L /* ROUGE_LSUM, ROUGE_1, ROUGE_2 if needed */ }
    public enum Aggregation { F1, PRECISION, RECALL }

    public RougeConfig {
        Objects.requireNonNull(variant, "variant must not be null");
        Objects.requireNonNull(aggregation, "aggregation must not be null");
    }
}