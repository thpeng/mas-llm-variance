package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoConfig;
import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceConfig;
import ch.thp.mas.llm.variance.analyze.route.RouteConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import java.util.Objects;

public record AnalysisConfig(
        ClusteringAlgorithm clusteringAlgorithm,
        RouteConfig route,
        FactualTravelInfoConfig factualTravelInfo,
        LiteralFormatTravelerGuidanceConfig literalFormatTravelerGuidance,
        CreativeMarketingTextConfig creativeMarketingText,
        BleuConfig bleu,
        RougeConfig rouge
) {

    public AnalysisConfig {
        Objects.requireNonNull(clusteringAlgorithm, "clusteringAlgorithm must not be null");
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(factualTravelInfo, "factualTravelInfo must not be null");
        Objects.requireNonNull(literalFormatTravelerGuidance, "literalFormatTravelerGuidance must not be null");
        Objects.requireNonNull(creativeMarketingText, "creativeMarketingText must not be null");
        Objects.requireNonNull(bleu, "bleu must not be null");
        Objects.requireNonNull(rouge, "rouge must not be null");
    }

    public static AnalysisConfig defaults() {
        ClusteringAlgorithm clusteringAlgorithm = clusteringAlgorithm(
                getenv("LLM_VARIANCE_CLUSTERING_ALGORITHM", "route"));
        return new AnalysisConfig(
                clusteringAlgorithm,
                new RouteConfig(5),
                new FactualTravelInfoConfig("08:02", "09:15", 0),
                new LiteralFormatTravelerGuidanceConfig(
                        "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3."),
                new CreativeMarketingTextConfig(3, "Luzern"),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
        );
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static ClusteringAlgorithm clusteringAlgorithm(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "route" -> ClusteringAlgorithm.ROUTE;
            case "factual-travel-info", "factual_travel_info" -> ClusteringAlgorithm.FACTUAL_TRAVEL_INFO;
            case "literal-format-traveler-guidance", "literal_format_traveler_guidance" ->
                    ClusteringAlgorithm.LITERAL_FORMAT_TRAVELER_GUIDANCE;
            case "creative-marketing-text", "creative_marketing_text" -> ClusteringAlgorithm.CREATIVE_MARKETING_TEXT;
            default -> throw new AnalysisException("Unknown clustering algorithm: " + value);
        };
    }
}
