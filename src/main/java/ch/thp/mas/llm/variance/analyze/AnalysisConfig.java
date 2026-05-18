package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.DistanceMetric;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.ScanRange;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticDistanceMethod;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import java.util.Objects;

/**
 * Top-level configuration for the variance analysis pipeline.
 *
 * <p>Bundles the embedding model parameters, the distance metric, and the
 * configurations of all downstream analysis components (semantic clustering,
 * BLEU and ROUGE surface metrics, and percentile aggregation).
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@code embeddingModel}, {@code embeddingPrefix},
 *       {@code maxEmbeddingTokens}: parameters for embedding generation. The
 *       {@code embeddingPrefix} convention follows the E5 family
 *       (Wang et al., 2022), which expects {@code "passage: "} or
 *       {@code "query: "} prefixes; for embedding models without this
 *       convention the prefix may be set to the empty string.
 *       {@code maxEmbeddingTokens} caps input length to the model's context
 *       window (e.g. 512 + 2 special tokens for BERT-family models).</li>
 *   <li>{@code distance}: distance metric used for the semantic analysis
 *       pipeline (cosine distance over embeddings).</li>
 *   <li>{@code dbscan}, {@code hierarchical}, {@code bleu}, {@code rouge}: see the corresponding
 *       config records.</li>
 *   <li>{@code percentile}: method used to compute percentile aggregates of
 *       pairwise distances (median, p90, etc.) within clusters.</li>
 * </ul>
 *
 * <p>The {@link #defaults()} factory provides a baseline configuration. See
 * its Javadoc for the open question regarding the default embedding model.
 */
public record AnalysisConfig(
        String embeddingProvider,
        String embeddingBaseUrl,
        String embeddingModel,
        String embeddingPrefix,
        int maxEmbeddingTokens,
        SemanticDistanceMethod semanticDistanceMethod,
        SemanticRepresentation semanticRepresentation,
        ChunkConfig chunk,
        DistanceMetric distance,
        ClusteringAlgorithm clusteringAlgorithm,
        double scanIncrement,
        DbscanConfig dbscan,
        HierarchicalConfig hierarchical,
        BleuConfig bleu,
        RougeConfig rouge,
        PercentileMethod percentile
) {

    public AnalysisConfig {
        if (embeddingProvider == null || embeddingProvider.isBlank()) {
            throw new IllegalArgumentException("embeddingProvider must not be blank");
        }
        if (embeddingBaseUrl == null || embeddingBaseUrl.isBlank()) {
            throw new IllegalArgumentException("embeddingBaseUrl must not be blank");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        Objects.requireNonNull(embeddingPrefix, "embeddingPrefix must not be null");
        if (maxEmbeddingTokens < 1) {
            throw new IllegalArgumentException("maxEmbeddingTokens must be at least 1");
        }
        Objects.requireNonNull(semanticDistanceMethod, "semanticDistanceMethod must not be null");
        Objects.requireNonNull(semanticRepresentation, "semanticRepresentation must not be null");
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(distance, "distance must not be null");
        Objects.requireNonNull(clusteringAlgorithm, "clusteringAlgorithm must not be null");
        ScanRange.incrementHundredths(scanIncrement);
        Objects.requireNonNull(dbscan, "dbscan must not be null");
        Objects.requireNonNull(hierarchical, "hierarchical must not be null");
        Objects.requireNonNull(bleu, "bleu must not be null");
        Objects.requireNonNull(rouge, "rouge must not be null");
        Objects.requireNonNull(percentile, "percentile must not be null");
    }

    /**
     * Baseline configuration for the analysis pipeline.
     *
     * <p>The default provider is the WSL-hosted HTTP service backed by
     * {@code intfloat/multilingual-e5-large}. For local development and CI,
     * set {@code LLM_VARIANCE_EMBEDDING_PROVIDER=local-hashing} to use the
     * deterministic fallback implementation.
     *
     * @return a baseline {@code AnalysisConfig} suitable for local development
     */
    public static AnalysisConfig defaults() {
        String provider = getenv("LLM_VARIANCE_EMBEDDING_PROVIDER", "e5-http");
        String baseUrl = getenv("LLM_VARIANCE_EMBEDDING_BASE_URL", "http://localhost:8000");
        SemanticDistanceMethod semanticDistanceMethod = semanticDistanceMethod(
                getenv("LLM_VARIANCE_SEMANTIC_DISTANCE_METHOD", "embedding-cosine"));
        SemanticRepresentation semanticRepresentation = semanticRepresentation(
                getenv("LLM_VARIANCE_SEMANTIC_REPRESENTATION", "full-text"));
        ClusteringAlgorithm clusteringAlgorithm = clusteringAlgorithm(
                getenv("LLM_VARIANCE_CLUSTERING_ALGORITHM", "hierarchical"));
        HierarchicalLinkage linkage = hierarchicalLinkage(
                getenv("LLM_VARIANCE_HIERARCHICAL_LINKAGE", "complete"));
        double scanIncrement = doubleEnv("LLM_VARIANCE_SCAN_INCREMENT", 0.01);
        double hierarchicalThreshold = doubleEnv("LLM_VARIANCE_HIERARCHICAL_THRESHOLD", 0.08);
        return new AnalysisConfig(
                provider,
                baseUrl,
                "intfloat/multilingual-e5-large",
                "passage:",
                514,
                semanticDistanceMethod,
                semanticRepresentation,
                new ChunkConfig(integerEnv("LLM_VARIANCE_CHUNK_TARGET_TOKENS", 120)),
                DistanceMetric.COSINE,
                clusteringAlgorithm,
                scanIncrement,
                new DbscanConfig(ScanRange.ofHundredths(15, 15), 2),
                new HierarchicalConfig(
                        ScanRange.of(hierarchicalThreshold, hierarchicalThreshold, scanIncrement,
                                "analysis.hierarchical.threshold"),
                        linkage),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1),
                PercentileMethod.NEAREST_RANK
        );
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int integerEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private static double doubleEnv(String name, double fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    private static SemanticRepresentation semanticRepresentation(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "full-text" -> SemanticRepresentation.FULL_TEXT;
            case "chunk-average-min" -> SemanticRepresentation.CHUNK_AVERAGE_MIN;
            default -> throw new AnalysisException("Unknown semantic representation: " + value);
        };
    }

    private static SemanticDistanceMethod semanticDistanceMethod(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "embedding-cosine" -> SemanticDistanceMethod.EMBEDDING_COSINE;
            case "bertscore-f1" -> SemanticDistanceMethod.BERTSCORE_F1;
            default -> throw new AnalysisException("Unknown semantic distance method: " + value);
        };
    }

    private static ClusteringAlgorithm clusteringAlgorithm(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "dbscan" -> ClusteringAlgorithm.DBSCAN;
            case "hierarchical" -> ClusteringAlgorithm.HIERARCHICAL;
            default -> throw new AnalysisException("Unknown clustering algorithm: " + value);
        };
    }

    private static HierarchicalLinkage hierarchicalLinkage(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "complete" -> HierarchicalLinkage.COMPLETE;
            case "average" -> HierarchicalLinkage.AVERAGE;
            default -> throw new AnalysisException("Unknown hierarchical linkage: " + value);
        };
    }
}
