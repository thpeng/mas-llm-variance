package ch.thp.mas.llm.variance.analyze.semantic;

import ch.thp.mas.llm.variance.analyze.EmbeddingResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CosineDistance {

    public double distance(double[] a, double[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public double[][] pairwise(List<EmbeddingResult> embeddings) {
        int size = embeddings.size();
        double[][] distances = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                double distance = distance(embeddings.get(i).vector(), embeddings.get(j).vector());
                distances[i][j] = distance;
                distances[j][i] = distance;
            }
        }
        return distances;
    }
}
