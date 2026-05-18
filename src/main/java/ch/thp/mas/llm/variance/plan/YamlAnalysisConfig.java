package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.PercentileMethod;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.DistanceMetric;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticDistanceMethod;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;

public class YamlAnalysisConfig {

    private String embeddingProvider;
    private String embeddingBaseUrl;
    private String embeddingModel;
    private String embeddingPrefix;
    private Integer maxEmbeddingTokens;
    private SemanticDistanceMethod semanticDistanceMethod;
    private SemanticRepresentation semanticRepresentation;
    private Chunk chunk;
    private DistanceMetric distance;
    private ClusteringAlgorithm clusteringAlgorithm;
    private Dbscan dbscan;
    private Hierarchical hierarchical;
    private Bleu bleu;
    private Rouge rouge;
    private PercentileMethod percentile;

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingPrefix() {
        return embeddingPrefix;
    }

    public void setEmbeddingPrefix(String embeddingPrefix) {
        this.embeddingPrefix = embeddingPrefix;
    }

    public Integer getMaxEmbeddingTokens() {
        return maxEmbeddingTokens;
    }

    public void setMaxEmbeddingTokens(Integer maxEmbeddingTokens) {
        this.maxEmbeddingTokens = maxEmbeddingTokens;
    }

    public SemanticDistanceMethod getSemanticDistanceMethod() {
        return semanticDistanceMethod;
    }

    public void setSemanticDistanceMethod(SemanticDistanceMethod semanticDistanceMethod) {
        this.semanticDistanceMethod = semanticDistanceMethod;
    }

    public SemanticRepresentation getSemanticRepresentation() {
        return semanticRepresentation;
    }

    public void setSemanticRepresentation(SemanticRepresentation semanticRepresentation) {
        this.semanticRepresentation = semanticRepresentation;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public DistanceMetric getDistance() {
        return distance;
    }

    public void setDistance(DistanceMetric distance) {
        this.distance = distance;
    }

    public ClusteringAlgorithm getClusteringAlgorithm() {
        return clusteringAlgorithm;
    }

    public void setClusteringAlgorithm(ClusteringAlgorithm clusteringAlgorithm) {
        this.clusteringAlgorithm = clusteringAlgorithm;
    }

    public Dbscan getDbscan() {
        return dbscan;
    }

    public void setDbscan(Dbscan dbscan) {
        this.dbscan = dbscan;
    }

    public Hierarchical getHierarchical() {
        return hierarchical;
    }

    public void setHierarchical(Hierarchical hierarchical) {
        this.hierarchical = hierarchical;
    }

    public Bleu getBleu() {
        return bleu;
    }

    public void setBleu(Bleu bleu) {
        this.bleu = bleu;
    }

    public Rouge getRouge() {
        return rouge;
    }

    public void setRouge(Rouge rouge) {
        this.rouge = rouge;
    }

    public PercentileMethod getPercentile() {
        return percentile;
    }

    public void setPercentile(PercentileMethod percentile) {
        this.percentile = percentile;
    }

    public static class Chunk {
        private Integer targetTokens;

        public Integer getTargetTokens() {
            return targetTokens;
        }

        public void setTargetTokens(Integer targetTokens) {
            this.targetTokens = targetTokens;
        }
    }

    public static class Dbscan {
        private Double epsilon;
        private Integer minPts;

        public Double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(Double epsilon) {
            this.epsilon = epsilon;
        }

        public Integer getMinPts() {
            return minPts;
        }

        public void setMinPts(Integer minPts) {
            this.minPts = minPts;
        }
    }

    public static class Hierarchical {
        private Double threshold;
        private HierarchicalLinkage linkage;

        public Double getThreshold() {
            return threshold;
        }

        public void setThreshold(Double threshold) {
            this.threshold = threshold;
        }

        public HierarchicalLinkage getLinkage() {
            return linkage;
        }

        public void setLinkage(HierarchicalLinkage linkage) {
            this.linkage = linkage;
        }
    }

    public static class Bleu {
        private Integer maxN;
        private Double smoothingEpsilon;

        public Integer getMaxN() {
            return maxN;
        }

        public void setMaxN(Integer maxN) {
            this.maxN = maxN;
        }

        public Double getSmoothingEpsilon() {
            return smoothingEpsilon;
        }

        public void setSmoothingEpsilon(Double smoothingEpsilon) {
            this.smoothingEpsilon = smoothingEpsilon;
        }
    }

    public static class Rouge {
        private RougeConfig.Variant variant;
        private RougeConfig.Aggregation aggregation;

        public RougeConfig.Variant getVariant() {
            return variant;
        }

        public void setVariant(RougeConfig.Variant variant) {
            this.variant = variant;
        }

        public RougeConfig.Aggregation getAggregation() {
            return aggregation;
        }

        public void setAggregation(RougeConfig.Aggregation aggregation) {
            this.aggregation = aggregation;
        }
    }
}
