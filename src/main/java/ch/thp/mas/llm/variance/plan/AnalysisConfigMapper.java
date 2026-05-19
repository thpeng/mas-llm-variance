package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoConfig;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceConfig;
import ch.thp.mas.llm.variance.analyze.route.RouteConfig;
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
        if (yaml.getClusteringAlgorithm() == null) {
            throw new AnalysisException("Missing analysis.clusteringAlgorithm in plan: " + loadedPlan.filename());
        }

        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                yaml.getClusteringAlgorithm(),
                route(yaml, defaults),
                factualTravelInfo(yaml, defaults),
                literalFormatTravelerGuidance(yaml, defaults),
                creativeMarketingText(yaml, defaults),
                bleu(yaml, defaults),
                rouge(yaml, defaults)
        );
    }

    private static RouteConfig route(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Route route = yaml.getRoute();
        if (route == null) {
            return defaults.route();
        }
        return new RouteConfig(valueOrDefault(route.getExpectedStationCount(), defaults.route().expectedStationCount()));
    }

    private static FactualTravelInfoConfig factualTravelInfo(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.FactualTravelInfo factualTravelInfo = yaml.getFactualTravelInfo();
        if (factualTravelInfo == null) {
            return defaults.factualTravelInfo();
        }
        return new FactualTravelInfoConfig(
                valueOrDefault(factualTravelInfo.getDepartureFromBern(), defaults.factualTravelInfo().departureFromBern()),
                valueOrDefault(factualTravelInfo.getArrivalAtZurich(), defaults.factualTravelInfo().arrivalAtZurich()),
                valueOrDefault(factualTravelInfo.getChanges(), defaults.factualTravelInfo().changes())
        );
    }

    private static LiteralFormatTravelerGuidanceConfig literalFormatTravelerGuidance(
            YamlAnalysisConfig yaml,
            AnalysisConfig defaults
    ) {
        YamlAnalysisConfig.LiteralFormatTravelerGuidance config = yaml.getLiteralFormatTravelerGuidance();
        if (config == null) {
            return defaults.literalFormatTravelerGuidance();
        }
        return new LiteralFormatTravelerGuidanceConfig(
                valueOrDefault(config.getReference(), defaults.literalFormatTravelerGuidance().reference())
        );
    }

    private static CreativeMarketingTextConfig creativeMarketingText(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.CreativeMarketingText config = yaml.getCreativeMarketingText();
        if (config == null) {
            return defaults.creativeMarketingText();
        }
        return new CreativeMarketingTextConfig(
                valueOrDefault(config.getExpectedSentenceCount(),
                        defaults.creativeMarketingText().expectedSentenceCount()),
                valueOrDefault(config.getRequiredTerm(), defaults.creativeMarketingText().requiredTerm())
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
