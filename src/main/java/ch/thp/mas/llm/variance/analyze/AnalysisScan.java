package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;

public record AnalysisScan(
        ClusteringAlgorithm algorithm,
        String parameter,
        double value,
        int clusterCount,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic
) {
}
