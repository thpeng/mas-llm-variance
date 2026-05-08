package ch.thp.mas.llm.variance.analyze.semantic;


import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.EmbeddingResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChunkAverageMinDistance {

    private final CosineDistance cosineDistance;

    public ChunkAverageMinDistance(CosineDistance cosineDistance) {
        this.cosineDistance = cosineDistance;
    }

    public double distance(List<EmbeddingResult> left, List<EmbeddingResult> right) {
        if (left.isEmpty() || right.isEmpty()) {
            throw new AnalysisException("Chunk-based distance requires at least one chunk per answer.");
        }
        return (directedAverageMin(left, right) + directedAverageMin(right, left)) / 2.0;
    }

    private double directedAverageMin(List<EmbeddingResult> source, List<EmbeddingResult> target) {
        double sum = 0.0;
        for (EmbeddingResult sourceEmbedding : source) {
            double min = Double.POSITIVE_INFINITY;
            for (EmbeddingResult targetEmbedding : target) {
                min = Math.min(min, cosineDistance.distance(sourceEmbedding.vector(), targetEmbedding.vector()));
            }
            sum += min;
        }
        return sum / source.size();
    }
}
