package ch.thp.mas.llm.variance.analyze.literalformat;

public record LiteralFormatTravelerGuidanceConfig(
        String reference
) {

    public LiteralFormatTravelerGuidanceConfig {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
