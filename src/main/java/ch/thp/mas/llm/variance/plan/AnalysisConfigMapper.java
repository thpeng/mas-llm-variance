package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
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
                dbscan(yaml, defaults),
                hierarchical(yaml, defaults),
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

    private static DbscanConfig dbscan(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Dbscan dbscan = yaml.getDbscan();
        if (dbscan == null) {
            return defaults.dbscan();
        }
        return new DbscanConfig(
                valueOrDefault(dbscan.getEpsilon(), defaults.dbscan().epsilon()),
                valueOrDefault(dbscan.getMinPts(), defaults.dbscan().minPts())
        );
    }

    private static HierarchicalConfig hierarchical(YamlAnalysisConfig yaml, AnalysisConfig defaults) {
        YamlAnalysisConfig.Hierarchical hierarchical = yaml.getHierarchical();
        if (hierarchical == null) {
            return defaults.hierarchical();
        }
        return new HierarchicalConfig(
                valueOrDefault(hierarchical.getThreshold(), defaults.hierarchical().threshold()),
                valueOrDefault(hierarchical.getLinkage(), defaults.hierarchical().linkage())
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
