package ch.thp.mas.llm.variance.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisConfig(
        PromptEvaluation promptEvaluation,
        SwissRoundTripConfig swissRoundTrip,
        BernZurichConnectionConfig bernZurichConnection,
        TravelerGuidanceFormatConfig travelerGuidanceFormat,
        LucerneMarketingTextConfig lucerneMarketingText,
        BleuConfig bleu,
        RougeConfig rouge
) {

    public AnalysisConfig {
        Objects.requireNonNull(promptEvaluation, "promptEvaluation must not be null");
    }

    public static AnalysisConfig defaults() {
        PromptEvaluation promptEvaluation = promptEvaluation(
                getenv("LLM_VARIANCE_PROMPT_EVALUATION", "ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP"));
        return new AnalysisConfig(
                promptEvaluation,
                new SwissRoundTripConfig(5, PromptLanguage.DE),
                new BernZurichConnectionConfig("08:02", "09:15", 0),
                new TravelerGuidanceFormatConfig(
                        "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3."),
                new LucerneMarketingTextConfig(3, "Luzern"),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
        );
    }

    public AnalysisConfig visibleForResult() {
        return switch (promptEvaluation) {
            case ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP -> new AnalysisConfig(
                    promptEvaluation, swissRoundTrip, null, null, null, bleu, rouge);
            case FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION -> new AnalysisConfig(
                    promptEvaluation, null, bernZurichConnection, null, null, bleu, rouge);
            case LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE -> new AnalysisConfig(
                    promptEvaluation, null, null, travelerGuidanceFormat, null, null, null);
            case CREATIVE_GENERATIVE_LUCERNE_MARKETING -> new AnalysisConfig(
                    promptEvaluation, null, null, null, lucerneMarketingText, bleu, rouge);
        };
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static PromptEvaluation promptEvaluation(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "advisory-recommendation-swiss-round-trip", "advisory_recommendation_swiss_round_trip",
                    "swiss-round-trip", "swiss_round_trip", "ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP", "route" ->
                    PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP;
            case "factual-critical-bern-zurich-connection", "factual_critical_bern_zurich_connection",
                    "factual-travel-info", "factual_travel_info" ->
                    PromptEvaluation.FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION;
            case "literal-format-critical-traveler-guidance", "literal_format_critical_traveler_guidance",
                    "literal-format-traveler-guidance", "literal_format_traveler_guidance" ->
                    PromptEvaluation.LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE;
            case "creative-generative-lucerne-marketing", "creative_generative_lucerne_marketing",
                    "creative-marketing-text", "creative_marketing_text" ->
                    PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING;
            default -> throw new AnalysisException("Unknown prompt evaluation: " + value);
        };
    }
}
