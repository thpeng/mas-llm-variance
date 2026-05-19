package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextAnalysis;
import ch.thp.mas.llm.variance.analyze.creative.CreativeMarketingTextAnalyzer;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalysis;
import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalyzer;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceAnalysis;
import ch.thp.mas.llm.variance.analyze.literalformat.LiteralFormatTravelerGuidanceAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalysis;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteCluster;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Analyzer {

    private final RouteAnalyzer routeAnalyzer;
    private final FactualTravelInfoAnalyzer factualTravelInfoAnalyzer;
    private final LiteralFormatTravelerGuidanceAnalyzer literalFormatTravelerGuidanceAnalyzer;
    private final CreativeMarketingTextAnalyzer creativeMarketingTextAnalyzer;
    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;
    private final LiteralAnalyzer literalAnalyzer;
    private final SummaryStatistics summaryStatistics;
    private final SystemRunClock runClock;
    private final Supplier<AnalysisConfig> configSupplier;

    @Autowired
    public Analyzer(
            RouteAnalyzer routeAnalyzer,
            FactualTravelInfoAnalyzer factualTravelInfoAnalyzer,
            LiteralFormatTravelerGuidanceAnalyzer literalFormatTravelerGuidanceAnalyzer,
            CreativeMarketingTextAnalyzer creativeMarketingTextAnalyzer,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this(
                routeAnalyzer,
                factualTravelInfoAnalyzer,
                literalFormatTravelerGuidanceAnalyzer,
                creativeMarketingTextAnalyzer,
                rougeLMetric,
                bleuMetric,
                literalAnalyzer,
                summaryStatistics,
                runClock,
                AnalysisConfig::defaults
        );
    }

    Analyzer(
            RouteAnalyzer routeAnalyzer,
            FactualTravelInfoAnalyzer factualTravelInfoAnalyzer,
            LiteralFormatTravelerGuidanceAnalyzer literalFormatTravelerGuidanceAnalyzer,
            CreativeMarketingTextAnalyzer creativeMarketingTextAnalyzer,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock,
            Supplier<AnalysisConfig> configSupplier
    ) {
        this.routeAnalyzer = routeAnalyzer;
        this.factualTravelInfoAnalyzer = factualTravelInfoAnalyzer;
        this.literalFormatTravelerGuidanceAnalyzer = literalFormatTravelerGuidanceAnalyzer;
        this.creativeMarketingTextAnalyzer = creativeMarketingTextAnalyzer;
        this.rougeLMetric = rougeLMetric;
        this.bleuMetric = bleuMetric;
        this.literalAnalyzer = literalAnalyzer;
        this.summaryStatistics = summaryStatistics;
        this.runClock = runClock;
        this.configSupplier = configSupplier;
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog) {
        AnalysisConfig config = configSupplier.get();
        return analyze(namedRunLog, config);
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog, AnalysisConfig config) {
        RunLog runLog = namedRunLog.runLog();
        List<String> responses = runLog.repetitions().stream()
                .map(RunLogEntry::response)
                .toList();
        if (responses.isEmpty()) {
            throw new AnalysisException("Run log has no responses: " + namedRunLog.filename());
        }

        LiteralAnalysis literalAnalysis = literalAnalyzer.analyze(responses);
        return switch (config.clusteringAlgorithm()) {
            case ROUTE -> routeResult(namedRunLog, runLog, responses, config, literalAnalysis);
            case FACTUAL_TRAVEL_INFO -> factualResult(namedRunLog, runLog, responses, config, literalAnalysis);
            case LITERAL_FORMAT_TRAVELER_GUIDANCE -> literalFormatResult(
                    namedRunLog, runLog, responses, config, literalAnalysis);
            case CREATIVE_MARKETING_TEXT -> creativeResult(namedRunLog, runLog, responses, config, literalAnalysis);
        };
    }

    private AnalysisResult routeResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        RouteAnalysis routeAnalysis = routeAnalyzer.analyze(
                responses,
                config.route(),
                clusters -> new SyntacticAnalysis(routeSyntacticClusters(clusters, responses, config))
        );
        return result(namedRunLog, runLog, config, routeAnalysis, null, null, null, literalAnalysis);
    }

    private AnalysisResult factualResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        FactualTravelInfoAnalysis factualTravelInfoAnalysis = factualTravelInfoAnalyzer.analyze(
                responses,
                config.factualTravelInfo(),
                successIndices -> new SyntacticAnalysis(syntacticClusters(0, successIndices, responses, config))
        );
        return result(namedRunLog, runLog, config, null, factualTravelInfoAnalysis, null, null, literalAnalysis);
    }

    private AnalysisResult literalFormatResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        LiteralFormatTravelerGuidanceAnalysis literalFormatTravelerGuidanceAnalysis =
                literalFormatTravelerGuidanceAnalyzer.analyze(responses, config.literalFormatTravelerGuidance());
        return result(namedRunLog, runLog, config, null, null, literalFormatTravelerGuidanceAnalysis, null,
                literalAnalysis);
    }

    private AnalysisResult creativeResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        CreativeMarketingTextAnalysis creativeMarketingTextAnalysis = creativeMarketingTextAnalyzer.analyze(
                responses,
                config.creativeMarketingText(),
                successIndices -> new SyntacticAnalysis(syntacticClusters(0, successIndices, responses, config))
        );
        return result(namedRunLog, runLog, config, null, null, null, creativeMarketingTextAnalysis, literalAnalysis);
    }

    private AnalysisResult result(
            NamedRunLog namedRunLog,
            RunLog runLog,
            AnalysisConfig config,
            RouteAnalysis route,
            FactualTravelInfoAnalysis factualTravelInfo,
            LiteralFormatTravelerGuidanceAnalysis literalFormatTravelerGuidance,
            CreativeMarketingTextAnalysis creativeMarketingText,
            LiteralAnalysis literal
    ) {
        return new AnalysisResult(
                namedRunLog.filename(),
                runClock.now(),
                config,
                runInfo(runLog),
                route,
                factualTravelInfo,
                literalFormatTravelerGuidance,
                creativeMarketingText,
                literal
        );
    }

    private AnalysisRunInfo runInfo(RunLog runLog) {
        return new AnalysisRunInfo(
                runLog.planName(),
                runLog.inferenceProvider(),
                runLog.model(),
                runLog.modelVersion(),
                runLog.iterations(),
                runLog.config().temperature(),
                runLog.config().topP(),
                runLog.config().topK(),
                runLog.config().seed(),
                runLog.config().reasoning()
        );
    }

    private List<SyntacticCluster> routeSyntacticClusters(
            List<RouteCluster> routeClusters,
            List<String> responses,
            AnalysisConfig config
    ) {
        return routeClusters.stream()
                .map(cluster -> syntacticCluster(cluster.clusterId(), cluster.repetitionIndices(), responses, config))
                .toList();
    }

    private List<SyntacticCluster> syntacticClusters(
            int clusterId,
            List<Integer> successIndices,
            List<String> responses,
            AnalysisConfig config
    ) {
        if (successIndices.isEmpty()) {
            return List.of();
        }
        return List.of(syntacticCluster(clusterId, successIndices, responses, config));
    }

    private SyntacticCluster syntacticCluster(
            int clusterId,
            List<Integer> repetitionIndices,
            List<String> responses,
            AnalysisConfig config
    ) {
        List<Integer> indices = repetitionIndices.stream().map(index -> index - 1).toList();
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
                clusterId,
                rougeDistances.size(),
                summaryStatistics.summarize(rougeDistances),
                summaryStatistics.summarize(bleuDistances)
        );
    }
}
