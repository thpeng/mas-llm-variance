package ch.thp.mas.llm.variance.analyze.semantic;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import java.util.List;

public interface BertScoreService {

    BertScoreResult score(List<String> texts, AnalysisConfig config);
}
