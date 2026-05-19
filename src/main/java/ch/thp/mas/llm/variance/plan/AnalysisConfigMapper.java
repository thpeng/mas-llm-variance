package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import org.springframework.stereotype.Component;

@Component
public class AnalysisConfigMapper {

    public AnalysisConfig map(LoadedPlan loadedPlan) {
        YamlAnalysisConfig yaml = ((YamlPlan) loadedPlan.plan()).getAnalysis();
        if (yaml == null) {
            throw new AnalysisException("Missing analysis block in plan: " + loadedPlan.filename());
        }
        if (yaml.getPromptEvaluation() == null) {
            throw new AnalysisException("Missing analysis.promptEvaluation in plan: " + loadedPlan.filename());
        }

        AnalysisConfig defaults = AnalysisConfig.defaults();
        var promptEvaluation = yaml.getPromptEvaluation();
        return new AnalysisConfig(
                promptEvaluation,
                promptEvaluation == PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                        ? swissRoundTrip(yaml, defaults) : null,
                promptEvaluation == PromptEvaluation.FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION
                        ? bernZurichConnection(yaml, defaults) : null,
                promptEvaluation == PromptEvaluation.LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE
                        ? travelerGuidanceFormat(yaml, defaults) : null,
                promptEvaluation == PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING
                        ? lucerneMarketingText(yaml, defaults) : null,
                bleu(yaml, defaults),
                rouge(yaml, defaults)
        );
    }

    private static SwissRoundTripConfig swissRoundTrip(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.SwissRoundTrip swissRoundTrip = yaml.getSwissRoundTrip();
        if (swissRoundTrip == null) {
            return defaults.swissRoundTrip();
        }
        return new SwissRoundTripConfig(valueOrDefault(swissRoundTrip.getExpectedStationCount(), defaults.swissRoundTrip().expectedStationCount()));
    }

    private static BernZurichConnectionConfig bernZurichConnection(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.BernZurichConnection bernZurichConnection = yaml.getBernZurichConnection();
        if (bernZurichConnection == null) {
            return defaults.bernZurichConnection();
        }
        return new BernZurichConnectionConfig(
                valueOrDefault(bernZurichConnection.getDepartureFromBern(), defaults.bernZurichConnection().departureFromBern()),
                valueOrDefault(bernZurichConnection.getArrivalAtZurich(), defaults.bernZurichConnection().arrivalAtZurich()),
                valueOrDefault(bernZurichConnection.getChanges(), defaults.bernZurichConnection().changes())
        );
    }

    private static TravelerGuidanceFormatConfig travelerGuidanceFormat(
            YamlAnalysisConfig yaml,
            AnalysisConfig defaults
    ) {
        YamlAnalysisConfig.TravelerGuidanceFormat config = yaml.getTravelerGuidanceFormat();
        if (config == null) {
            return defaults.travelerGuidanceFormat();
        }
        return new TravelerGuidanceFormatConfig(
                valueOrDefault(config.getReference(), defaults.travelerGuidanceFormat().reference())
        );
    }

    private static LucerneMarketingTextConfig lucerneMarketingText(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.LucerneMarketingText config = yaml.getLucerneMarketingText();
        if (config == null) {
            return defaults.lucerneMarketingText();
        }
        return new LucerneMarketingTextConfig(
                valueOrDefault(config.getExpectedSentenceCount(),
                        defaults.lucerneMarketingText().expectedSentenceCount()),
                valueOrDefault(config.getRequiredTerm(), defaults.lucerneMarketingText().requiredTerm())
        );
    }

    private static BleuConfig bleu(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Bleu bleu = yaml.getBleu();
        if (bleu == null) {
            return defaults.bleu();
        }
        return new BleuConfig(
                valueOrDefault(bleu.getMaxN(), defaults.bleu().maxN()),
                valueOrDefault(bleu.getSmoothingEpsilon(), defaults.bleu().smoothingEpsilon())
        );
    }

    private static RougeConfig rouge(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Rouge rouge = yaml.getRouge();
        if (rouge == null) {
            return defaults.rouge();
        }
        return new RougeConfig(
                valueOrDefault(rouge.getVariant(), defaults.rouge().variant()),
                valueOrDefault(rouge.getAggregation(), defaults.rouge().aggregation())
        );
    }

    private static <T> T valueOrDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

}
