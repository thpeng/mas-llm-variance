package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;

public record NamedAnalysisResult(String filename, AnalysisResult analysisResult) {
}
