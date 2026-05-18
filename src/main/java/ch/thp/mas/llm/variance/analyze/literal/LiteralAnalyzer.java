package ch.thp.mas.llm.variance.analyze.literal;

import ch.thp.mas.llm.variance.analyze.semantic.SemanticCluster;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LiteralAnalyzer {

    public LiteralAnalysis analyze(List<String> responses, List<SemanticCluster> semanticClusters) {
        PairStats runStats = pairStats(responses);
        return new LiteralAnalysis(
                distinctResponseCount(responses) <= 1,
                responses.size(),
                distinctResponseCount(responses),
                runStats.exactMatchRate(),
                semanticClusters.stream()
                        .map(cluster -> clusterAnalysis(cluster, responses))
                        .toList()
        );
    }

    public LiteralAnalysis analyze(List<String> responses) {
        PairStats runStats = pairStats(responses);
        return new LiteralAnalysis(
                distinctResponseCount(responses) <= 1,
                responses.size(),
                distinctResponseCount(responses),
                runStats.exactMatchRate(),
                List.of()
        );
    }

    private LiteralClusterAnalysis clusterAnalysis(SemanticCluster cluster, List<String> responses) {
        List<String> clusterResponses = cluster.repetitionIndices().stream()
                .map(index -> responses.get(index - 1))
                .toList();
        PairStats stats = pairStats(clusterResponses);
        return new LiteralClusterAnalysis(
                cluster.clusterId(),
                clusterResponses.size(),
                stats.pairCount(),
                stats.exactMatchPairCount(),
                stats.exactMatchRate(),
                distinctResponseCount(clusterResponses)
        );
    }

    private int distinctResponseCount(List<String> responses) {
        Set<String> distinctResponses = new HashSet<>(responses);
        return distinctResponses.size();
    }

    private PairStats pairStats(List<String> responses) {
        int pairCount = 0;
        int exactMatchPairCount = 0;
        for (int i = 0; i < responses.size(); i++) {
            for (int j = i + 1; j < responses.size(); j++) {
                pairCount++;
                if (Objects.equals(responses.get(i), responses.get(j))) {
                    exactMatchPairCount++;
                }
            }
        }
        double exactMatchRate = pairCount == 0
                ? 1.0
                : (double) exactMatchPairCount / pairCount;
        return new PairStats(pairCount, exactMatchPairCount, exactMatchRate);
    }

    private record PairStats(int pairCount, int exactMatchPairCount, double exactMatchRate) {
    }
}
