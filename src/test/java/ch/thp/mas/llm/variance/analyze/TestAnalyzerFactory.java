package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluator;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripStationExtractor;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.run.SystemRunClock;

final class TestAnalyzerFactory {

    private TestAnalyzerFactory() {
    }

    static Analyzer create(AnalysisConfig config) {
        return create(config, new AnalyzerTest.FixedClock());
    }

    static Analyzer create(AnalysisConfig config, SystemRunClock clock) {
        TextTokenizer tokenizer = new TextTokenizer();
        return new Analyzer(
                new SwissRoundTripEvaluator(new SwissRoundTripStationExtractor()),
                new BernZurichConnectionEvaluator(),
                new TravelerGuidanceFormatEvaluator(),
                new LucerneMarketingTextEvaluator(),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                clock,
                () -> config
        );
    }
}
