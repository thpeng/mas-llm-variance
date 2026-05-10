package ch.thp.mas.llm.variance.analyze.semantic;

import java.util.List;

public record BertScoreResult(double[][] distances, List<PairScore> pairScores) {

    public record PairScore(int leftIndex, int rightIndex, BertScore score) {
    }
}
