package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalysis;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalyzer;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.Medoid;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidAnalysis;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticAnalysis;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticCluster;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticDistanceMethod;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalysis;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteCluster;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Analyzer {

    private final EmbeddingService embeddingService;
    private final CosineDistance cosineDistance;
    private final ChunkAverageMinDistance chunkAverageMinDistance;
    private final MedoidSelector medoidSelector;
    private final DbscanClusterer dbscanClusterer;
    private final HierarchicalClusterer hierarchicalClusterer;
    private final RouteAnalyzer routeAnalyzer;
    private final FactualTravelInfoAnalyzer factualTravelInfoAnalyzer;
    private final AnswerChunker answerChunker;
    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;
    private final LiteralAnalyzer literalAnalyzer;
    private final SummaryStatistics summaryStatistics;
    private final SystemRunClock runClock;
    private final Supplier<AnalysisConfig> configSupplier;

    @Autowired
    public Analyzer(
            EmbeddingService embeddingService,
            CosineDistance cosineDistance,
            ChunkAverageMinDistance chunkAverageMinDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            HierarchicalClusterer hierarchicalClusterer,
            RouteAnalyzer routeAnalyzer,
            FactualTravelInfoAnalyzer factualTravelInfoAnalyzer,
            AnswerChunker answerChunker,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this(
                embeddingService,
                cosineDistance,
                chunkAverageMinDistance,
                medoidSelector,
                dbscanClusterer,
                hierarchicalClusterer,
                routeAnalyzer,
                factualTravelInfoAnalyzer,
                answerChunker,
                rougeLMetric,
                bleuMetric,
                literalAnalyzer,
                summaryStatistics,
                runClock,
                AnalysisConfig::defaults
        );
    }

    Analyzer(
            EmbeddingService embeddingService,
            CosineDistance cosineDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this(
                embeddingService,
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                medoidSelector,
                dbscanClusterer,
                new HierarchicalClusterer(),
                new RouteAnalyzer(new ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor()),
                new FactualTravelInfoAnalyzer(),
                new AnswerChunker(new TextTokenizer()),
                rougeLMetric,
                bleuMetric,
                new LiteralAnalyzer(),
                summaryStatistics,
                runClock,
                AnalysisConfig::defaults
        );
    }

    Analyzer(
            EmbeddingService embeddingService,
            CosineDistance cosineDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock,
            Supplier<AnalysisConfig> configSupplier
    ) {
        this(
                embeddingService,
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                medoidSelector,
                dbscanClusterer,
                new HierarchicalClusterer(),
                new RouteAnalyzer(new ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor()),
                new FactualTravelInfoAnalyzer(),
                new AnswerChunker(new TextTokenizer()),
                rougeLMetric,
                bleuMetric,
                new LiteralAnalyzer(),
                summaryStatistics,
                runClock,
                configSupplier
        );
    }

    Analyzer(
            EmbeddingService embeddingService,
            CosineDistance cosineDistance,
            ChunkAverageMinDistance chunkAverageMinDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            HierarchicalClusterer hierarchicalClusterer,
            RouteAnalyzer routeAnalyzer,
            FactualTravelInfoAnalyzer factualTravelInfoAnalyzer,
            AnswerChunker answerChunker,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock,
            Supplier<AnalysisConfig> configSupplier
    ) {
        this.embeddingService = embeddingService;
        this.cosineDistance = cosineDistance;
        this.chunkAverageMinDistance = chunkAverageMinDistance;
        this.medoidSelector = medoidSelector;
        this.dbscanClusterer = dbscanClusterer;
        this.hierarchicalClusterer = hierarchicalClusterer;
        this.routeAnalyzer = routeAnalyzer;
        this.factualTravelInfoAnalyzer = factualTravelInfoAnalyzer;
        this.answerChunker = answerChunker;
        this.rougeLMetric = rougeLMetric;
        this.bleuMetric = bleuMetric;
        this.literalAnalyzer = literalAnalyzer;
        this.summaryStatistics = summaryStatistics;
        this.runClock = runClock;
        this.configSupplier = configSupplier;
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog) {
        AnalysisConfig config = configSupplier.get();
        return analyze(namedRunLog, config);
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog, AnalysisConfig config) {
        RunLog runLog = namedRunLog.runLog();
        List<String> responses = runLog.repetitions().stream()
                .map(RunLogEntry::response)
                .toList();
        if (responses.isEmpty()) {
            throw new AnalysisException("Run log has no responses: " + namedRunLog.filename());
        }

        LiteralAnalysis literalAnalysis = literalAnalyzer.analyze(responses);
        if (config.clusteringAlgorithm() == ClusteringAlgorithm.ROUTE) {
            RouteAnalysis routeAnalysis = routeAnalyzer.analyze(
                    responses,
                    config.route(),
                    clusters -> new SyntacticAnalysis(routeSyntacticClusters(clusters, responses, config))
            );
            return new AnalysisResult(
                    namedRunLog.filename(),
                    runClock.now(),
                    config,
                    runInfo(runLog),
                    List.of(),
                    routeAnalysis,
                    null,
                    literalAnalysis
            );
        }

        if (config.clusteringAlgorithm() == ClusteringAlgorithm.FACTUAL_TRAVEL_INFO) {
            FactualTravelInfoAnalysis factualTravelInfoAnalysis = factualTravelInfoAnalyzer.analyze(
                    responses,
                    config.factualTravelInfo(),
                    successIndices -> new SyntacticAnalysis(factualSyntacticClusters(successIndices, responses, config))
            );
            return new AnalysisResult(
                    namedRunLog.filename(),
                    runClock.now(),
                    config,
                    runInfo(runLog),
                    List.of(),
                    null,
                    factualTravelInfoAnalysis,
                    literalAnalysis
            );
        }

        SemanticEmbeddings semanticEmbeddings = semanticEmbeddings(responses, config);
        double[][] distances = semanticEmbeddings.distances();
        Medoid medoid = medoidSelector.select(distances);
        MedoidAnalysis medoidAnalysis = new MedoidAnalysis(
                medoid.index() + 1,
                medoid.totalDistance(),
                responses.get(medoid.index())
        );
        MetricSummary pairwiseSummary = summaryStatistics.summarize(upperTriangle(distances));
        List<AnalysisScan> scans = scans(responses, config, semanticEmbeddings, distances, medoidAnalysis, pairwiseSummary);

        return new AnalysisResult(
                namedRunLog.filename(),
                runClock.now(),
                config,
                runInfo(runLog),
                scans,
                null,
                null,
                literalAnalysis
        );
    }

    private List<AnalysisScan> scans(
            List<String> responses,
            AnalysisConfig config,
            SemanticEmbeddings semanticEmbeddings,
            double[][] distances,
            MedoidAnalysis medoidAnalysis,
            MetricSummary pairwiseSummary
    ) {
        return switch (config.clusteringAlgorithm()) {
            case DBSCAN -> config.dbscan().epsilon().values(config.scanIncrement()).stream()
                    .map(value -> scan(
                            ClusteringAlgorithm.DBSCAN,
                            "epsilon",
                            value,
                            dbscanClusterer.cluster(distances, value, config.dbscan().minPts()),
                            responses,
                            config,
                            semanticEmbeddings,
                            distances,
                            medoidAnalysis,
                            pairwiseSummary))
                    .toList();
            case HIERARCHICAL -> config.hierarchical().threshold().values(config.scanIncrement()).stream()
                    .map(value -> scan(
                            ClusteringAlgorithm.HIERARCHICAL,
                            "threshold",
                            value,
                            hierarchicalClusterer.cluster(distances, value, config.hierarchical().linkage()),
                            responses,
                            config,
                            semanticEmbeddings,
                            distances,
                            medoidAnalysis,
                            pairwiseSummary))
                    .toList();
            case ROUTE -> throw new AnalysisException("ROUTE clustering does not use semantic scan entries.");
            case FACTUAL_TRAVEL_INFO -> throw new AnalysisException(
                    "FACTUAL_TRAVEL_INFO clustering does not use semantic scan entries.");
        };
    }

    private AnalysisScan scan(
            ClusteringAlgorithm algorithm,
            String parameter,
            double value,
            int[] labels,
            List<String> responses,
            AnalysisConfig config,
            SemanticEmbeddings semanticEmbeddings,
            double[][] distances,
            MedoidAnalysis medoidAnalysis,
            MetricSummary pairwiseSummary
    ) {
        List<SemanticCluster> semanticClusters = semanticClusters(labels, distances);
        SemanticAnalysis semanticAnalysis = new SemanticAnalysis(
                responses.size(),
                semanticEmbeddings.truncatedResponses(),
                medoidAnalysis,
                pairwiseSummary,
                semanticClusters,
                outliers(labels)
        );
        SyntacticAnalysis syntacticAnalysis = new SyntacticAnalysis(syntacticClusters(semanticClusters, responses, config));
        return new AnalysisScan(algorithm, parameter, value, semanticClusters.size(), semanticAnalysis, syntacticAnalysis);
    }

    private SemanticEmbeddings semanticEmbeddings(List<String> responses, AnalysisConfig config) {
        if (config.semanticDistanceMethod() == SemanticDistanceMethod.BERTSCORE_F1) {
            throw new AnalysisException("BERTScore/RoBERTa semantic distance is deprecated and no longer supported. "
                    + "Use embedding cosine distance with E5 chunking and hierarchical clustering instead.");
        }
        if (config.semanticRepresentation() == SemanticRepresentation.FULL_TEXT) {
            List<EmbeddingResult> embeddings = embeddingService.embed(responses, config);
            return new SemanticEmbeddings(
                    cosineDistance.pairwise(embeddings),
                    (int) embeddings.stream().filter(EmbeddingResult::truncated).count()
            );
        }
        return chunkAverageMinEmbeddings(responses, config);
    }

    private SemanticEmbeddings chunkAverageMinEmbeddings(List<String> responses, AnalysisConfig config) {
        List<List<String>> chunksByResponse = responses.stream()
                .map(response -> answerChunker.chunk(response, config.chunk(), config.maxEmbeddingTokens()))
                .toList();
        List<String> flattenedChunks = chunksByResponse.stream()
                .flatMap(List::stream)
                .toList();
        List<EmbeddingResult> flattenedEmbeddings = embeddingService.embed(flattenedChunks, config);

        List<List<EmbeddingResult>> embeddingsByResponse = new ArrayList<>();
        int offset = 0;
        int truncatedResponses = 0;
        for (List<String> chunks : chunksByResponse) {
            List<EmbeddingResult> responseEmbeddings = flattenedEmbeddings.subList(offset, offset + chunks.size());
            embeddingsByResponse.add(responseEmbeddings);
            if (responseEmbeddings.stream().anyMatch(EmbeddingResult::truncated)) {
                truncatedResponses++;
            }
            offset += chunks.size();
        }

        double[][] distances = new double[responses.size()][responses.size()];
        for (int i = 0; i < responses.size(); i++) {
            for (int j = i + 1; j < responses.size(); j++) {
                double distance = chunkAverageMinDistance.distance(embeddingsByResponse.get(i), embeddingsByResponse.get(j));
                distances[i][j] = distance;
                distances[j][i] = distance;
            }
        }
        return new SemanticEmbeddings(distances, truncatedResponses);
    }

    private AnalysisRunInfo runInfo(RunLog runLog) {
        return new AnalysisRunInfo(
                runLog.planName(),
                runLog.inferenceProvider(),
                runLog.model(),
                runLog.modelVersion(),
                runLog.iterations(),
                runLog.config().temperature(),
                runLog.config().topP(),
                runLog.config().topK(),
                runLog.config().seed(),
                runLog.config().reasoning()
        );
    }

    private List<SemanticCluster> semanticClusters(int[] labels, double[][] distances) {
        Map<Integer, List<Integer>> byCluster = new LinkedHashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] >= 0) {
                byCluster.computeIfAbsent(labels[i], ignored -> new ArrayList<>()).add(i);
            }
        }
        return byCluster.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> semanticCluster(entry.getKey(), entry.getValue(), distances))
                .toList();
    }

    private SemanticCluster semanticCluster(int clusterId, List<Integer> indices, double[][] distances) {
        Integer medoidIndex = indices.size() > 1
                ? clusterMedoid(indices, distances) + 1
                : indices.getFirst() + 1;
        return new SemanticCluster(
                clusterId,
                indices.size(),
                indices.stream().map(index -> index + 1).toList(),
                medoidIndex,
                summaryStatistics.summarize(pairDistances(indices, distances))
        );
    }

    private int clusterMedoid(List<Integer> indices, double[][] distances) {
        return indices.stream()
                .min(Comparator.comparingDouble(index -> indices.stream()
                        .mapToDouble(other -> distances[index][other])
                        .sum()))
                .orElseThrow();
    }

    private List<SyntacticCluster> syntacticClusters(
            List<SemanticCluster> semanticClusters,
            List<String> responses,
            AnalysisConfig config
    ) {
        return semanticClusters.stream()
                .map(cluster -> syntacticCluster(cluster, responses, config))
                .toList();
    }

    private List<SyntacticCluster> routeSyntacticClusters(
            List<RouteCluster> routeClusters,
            List<String> responses,
            AnalysisConfig config
    ) {
        return routeClusters.stream()
                .map(cluster -> syntacticCluster(cluster.clusterId(), cluster.repetitionIndices(), responses, config))
                .toList();
    }

    private SyntacticCluster syntacticCluster(SemanticCluster cluster, List<String> responses, AnalysisConfig config) {
        return syntacticCluster(cluster.clusterId(), cluster.repetitionIndices(), responses, config);
    }

    private List<SyntacticCluster> factualSyntacticClusters(
            List<Integer> successIndices,
            List<String> responses,
            AnalysisConfig config
    ) {
        if (successIndices.isEmpty()) {
            return List.of();
        }
        return List.of(syntacticCluster(0, successIndices, responses, config));
    }

    private SyntacticCluster syntacticCluster(
            int clusterId,
            List<Integer> repetitionIndices,
            List<String> responses,
            AnalysisConfig config
    ) {
        List<Integer> indices = repetitionIndices.stream().map(index -> index - 1).toList();
        List<Double> rougeDistances = new ArrayList<>();
        List<Double> bleuDistances = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                String left = responses.get(indices.get(i));
                String right = responses.get(indices.get(j));
                rougeDistances.add(1.0 - rougeLMetric.score(left, right));
                bleuDistances.add(1.0 - bleuMetric.score(left, right, config.bleu()));
            }
        }
        return new SyntacticCluster(
                clusterId,
                rougeDistances.size(),
                summaryStatistics.summarize(rougeDistances),
                summaryStatistics.summarize(bleuDistances)
        );
    }

    private List<Integer> outliers(int[] labels) {
        List<Integer> outliers = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] < 0) {
                outliers.add(i + 1);
            }
        }
        return outliers;
    }

    private List<Double> upperTriangle(double[][] distances) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            for (int j = i + 1; j < distances.length; j++) {
                values.add(distances[i][j]);
            }
        }
        return values;
    }

    private List<Double> pairDistances(List<Integer> indices, double[][] distances) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                values.add(distances[indices.get(i)][indices.get(j)]);
            }
        }
        return values;
    }

    private record SemanticEmbeddings(double[][] distances, int truncatedResponses) {
    }
}
