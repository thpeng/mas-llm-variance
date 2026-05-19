package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoConfig;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ScanRange;
import ch.thp.mas.llm.variance.analyze.route.RouteConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import java.util.Map;
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
        double scanIncrement = valueOrDefault(yaml.getScanIncrement(), defaults.scanIncrement());
        return new AnalysisConfig(
                valueOrDefault(yaml.getEmbeddingProvider(), defaults.embeddingProvider()),
                valueOrDefault(yaml.getEmbeddingBaseUrl(), defaults.embeddingBaseUrl()),
                valueOrDefault(yaml.getEmbeddingModel(), defaults.embeddingModel()),
                valueOrDefault(yaml.getEmbeddingPrefix(), defaults.embeddingPrefix()),
                valueOrDefault(yaml.getMaxEmbeddingTokens(), defaults.maxEmbeddingTokens()),
                valueOrDefault(yaml.getSemanticDistanceMethod(), defaults.semanticDistanceMethod()),
                valueOrDefault(yaml.getSemanticRepresentation(), defaults.semanticRepresentation()),
                chunk(yaml, defaults),
                valueOrDefault(yaml.getDistance(), defaults.distance()),
                yaml.getClusteringAlgorithm(),
                scanIncrement,
                dbscan(yaml, defaults, scanIncrement),
                hierarchical(yaml, defaults, scanIncrement),
                route(yaml, defaults),
                factualTravelInfo(yaml, defaults),
                literalFormatTravelerGuidance(yaml, defaults),
                bleu(yaml, defaults),
                rouge(yaml, defaults),
                valueOrDefault(yaml.getPercentile(), defaults.percentile())
        );
    }

    private static ChunkConfig chunk(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Chunk chunk = yaml.getChunk();
        if (chunk == null) {
            return defaults.chunk();
        }
        return new ChunkConfig(valueOrDefault(chunk.getTargetTokens(), defaults.chunk().targetTokens()));
    }

    private static DbscanConfig dbscan(YamlAnalysisConfig yaml, AnalysisConfig defaults, double scanIncrement) {
        YamlAnalysisConfig.Dbscan dbscan = yaml.getDbscan();
        if (dbscan == null) {
            return defaults.dbscan();
        }
        return new DbscanConfig(
                rangeOrDefault(dbscan.getEpsilon(), defaults.dbscan().epsilon(), scanIncrement, "analysis.dbscan.epsilon"),
                valueOrDefault(dbscan.getMinPts(), defaults.dbscan().minPts())
        );
    }

    private static HierarchicalConfig hierarchical(YamlAnalysisConfig yaml, AnalysisConfig defaults, double scanIncrement) {
        YamlAnalysisConfig.Hierarchical hierarchical = yaml.getHierarchical();
        if (hierarchical == null) {
            return defaults.hierarchical();
        }
        return new HierarchicalConfig(
                rangeOrDefault(hierarchical.getThreshold(), defaults.hierarchical().threshold(), scanIncrement,
                        "analysis.hierarchical.threshold"),
                valueOrDefault(hierarchical.getLinkage(), defaults.hierarchical().linkage())
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

    private static ScanRange rangeOrDefault(Object raw, ScanRange fallback, double scanIncrement, String name) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number) {
            throw new AnalysisException(name + " must be a range with from/to, not a scalar value.");
        }
        Double from;
        Double to;
        if (raw instanceof YamlAnalysisConfig.Range range) {
            from = range.getFrom();
            to = range.getTo();
        } else if (raw instanceof Map<?, ?> map) {
            from = numberValue(map.get("from"), name + ".from");
            to = numberValue(map.get("to"), name + ".to");
        } else {
            throw new AnalysisException(name + " must be a range with from/to.");
        }
        if (from == null) {
            throw new AnalysisException("Missing " + name + ".from");
        }
        if (to == null) {
            throw new AnalysisException("Missing " + name + ".to");
        }
        return ScanRange.of(from, to, scanIncrement, name);
    }

    private static Double numberValue(Object raw, String name) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        throw new AnalysisException(name + " must be numeric.");
    }
}
