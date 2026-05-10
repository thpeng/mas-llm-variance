package ch.thp.mas.llm.variance.analyze;


import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreResult;
import ch.thp.mas.llm.variance.analyze.semantic.BertScoreService;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
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
    private final BertScoreService bertScoreService;
    private final CosineDistance cosineDistance;
    private final ChunkAverageMinDistance chunkAverageMinDistance;
    private final MedoidSelector medoidSelector;
    private final DbscanClusterer dbscanClusterer;
    private final HierarchicalClusterer hierarchicalClusterer;
    private final AnswerChunker answerChunker;
    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;
    private final SummaryStatistics summaryStatistics;
    private final SystemRunClock runClock;
    private final Supplier<AnalysisConfig> configSupplier;

    @Autowired
    public Analyzer(
            EmbeddingService embeddingService,
            BertScoreService bertScoreService,
            CosineDistance cosineDistance,
            ChunkAverageMinDistance chunkAverageMinDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            HierarchicalClusterer hierarchicalClusterer,
            AnswerChunker answerChunker,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this(
                embeddingService,
                bertScoreService,
                cosineDistance,
                chunkAverageMinDistance,
                medoidSelector,
                dbscanClusterer,
                hierarchicalClusterer,
                answerChunker,
                rougeLMetric,
                bleuMetric,
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
                (texts, config) -> {
                    throw new AnalysisException("BERTScore service is not configured.");
                },
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                medoidSelector,
                dbscanClusterer,
                new HierarchicalClusterer(),
                new AnswerChunker(new TextTokenizer()),
                rougeLMetric,
                bleuMetric,
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
                (texts, config) -> {
                    throw new AnalysisException("BERTScore service is not configured.");
                },
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                medoidSelector,
                dbscanClusterer,
                new HierarchicalClusterer(),
                new AnswerChunker(new TextTokenizer()),
                rougeLMetric,
                bleuMetric,
                summaryStatistics,
                runClock,
                configSupplier
        );
    }

    Analyzer(
            EmbeddingService embeddingService,
            BertScoreService bertScoreService,
            CosineDistance cosineDistance,
            ChunkAverageMinDistance chunkAverageMinDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            HierarchicalClusterer hierarchicalClusterer,
            AnswerChunker answerChunker,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock,
            Supplier<AnalysisConfig> configSupplier
    ) {
        this.embeddingService = embeddingService;
        this.bertScoreService = bertScoreService;
        this.cosineDistance = cosineDistance;
        this.chunkAverageMinDistance = chunkAverageMinDistance;
        this.medoidSelector = medoidSelector;
        this.dbscanClusterer = dbscanClusterer;
        this.hierarchicalClusterer = hierarchicalClusterer;
        this.answerChunker = answerChunker;
        this.rougeLMetric = rougeLMetric;
        this.bleuMetric = bleuMetric;
        this.summaryStatistics = summaryStatistics;
        this.runClock = runClock;
        this.configSupplier = configSupplier;
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog) {
        AnalysisConfig config = configSupplier.get();
        RunLog runLog = namedRunLog.runLog();
        List<String> responses = runLog.repetitions().stream()
                .map(RunLogEntry::response)
                .toList();
        if (responses.isEmpty()) {
            throw new AnalysisException("Run log has no responses: " + namedRunLog.filename());
        }

        SemanticEmbeddings semanticEmbeddings = semanticEmbeddings(responses, config);
        double[][] distances = semanticEmbeddings.distances();
        Medoid medoid = medoidSelector.select(distances);
        int[] labels = cluster(distances, config);
        List<SemanticCluster> semanticClusters = semanticClusters(labels, distances);

        return new AnalysisResult(
                namedRunLog.filename(),
                runClock.now(),
                config,
                runInfo(runLog),
                new SemanticAnalysis(
                        responses.size(),
                        semanticEmbeddings.truncatedResponses(),
                        new MedoidAnalysis(
                                medoid.index() + 1,
                                medoid.totalDistance(),
                                responses.get(medoid.index())
                        ),
                        summaryStatistics.summarize(upperTriangle(distances)),
                        semanticClusters,
                        outliers(labels)
                ),
                new SyntacticAnalysis(syntacticClusters(semanticClusters, responses, config))
        );
    }

    private SemanticEmbeddings semanticEmbeddings(List<String> responses, AnalysisConfig config) {
        if (config.semanticDistanceMethod() == SemanticDistanceMethod.BERTSCORE_F1) {
            BertScoreResult result = bertScoreService.score(responses, config);
            return new SemanticEmbeddings(result.distances(), 0);
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

    private int[] cluster(double[][] distances, AnalysisConfig config) {
        return switch (config.clusteringAlgorithm()) {
            case DBSCAN -> dbscanClusterer.cluster(distances, config.dbscan());
            case HIERARCHICAL -> hierarchicalClusterer.cluster(distances, config.hierarchical());
        };
    }

    private AnalysisRunInfo runInfo(RunLog runLog) {
        return new AnalysisRunInfo(
                runLog.planName(),
                runLog.manufacturer(),
                runLog.model(),
                runLog.modelVersion(),
                runLog.iterations(),
                runLog.config().temperature(),
                runLog.config().topP(),
                runLog.config().topK(),
                runLog.config().seed()
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

    private SyntacticCluster syntacticCluster(SemanticCluster cluster, List<String> responses, AnalysisConfig config) {
        List<Integer> indices = cluster.repetitionIndices().stream().map(index -> index - 1).toList();
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
                cluster.clusterId(),
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
