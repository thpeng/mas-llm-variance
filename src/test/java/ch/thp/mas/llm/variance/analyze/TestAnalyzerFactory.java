package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextAnalyzer;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalyzer;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor;
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
                new RouteAnalyzer(new RouteStationExtractor()),
                new FactualTravelInfoAnalyzer(),
                new LiteralFormatTravelerGuidanceAnalyzer(),
                new CreativeMarketingTextAnalyzer(),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                clock,
                () -> config
        );
    }
}
