package ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical;

public record TravelerGuidanceFormatConfig(
        String reference
) {

    public TravelerGuidanceFormatConfig {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
