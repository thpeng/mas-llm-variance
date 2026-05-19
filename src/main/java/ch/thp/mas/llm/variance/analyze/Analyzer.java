package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluator;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluator;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripCluster;
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

    private final SwissRoundTripEvaluator swissRoundTripEvaluator;
    private final BernZurichConnectionEvaluator bernZurichConnectionEvaluator;
    private final TravelerGuidanceFormatEvaluator travelerGuidanceFormatEvaluator;
    private final LucerneMarketingTextEvaluator lucerneMarketingTextEvaluator;
    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;
    private final LiteralAnalyzer literalAnalyzer;
    private final SummaryStatistics summaryStatistics;
    private final SystemRunClock runClock;
    private final Supplier<AnalysisConfig> configSupplier;

    @Autowired
    public Analyzer(
            SwissRoundTripEvaluator swissRoundTripEvaluator,
            BernZurichConnectionEvaluator bernZurichConnectionEvaluator,
            TravelerGuidanceFormatEvaluator travelerGuidanceFormatEvaluator,
            LucerneMarketingTextEvaluator lucerneMarketingTextEvaluator,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this(
                swissRoundTripEvaluator,
                bernZurichConnectionEvaluator,
                travelerGuidanceFormatEvaluator,
                lucerneMarketingTextEvaluator,
                rougeLMetric,
                bleuMetric,
                literalAnalyzer,
                summaryStatistics,
                runClock,
                AnalysisConfig::defaults
        );
    }

    Analyzer(
            SwissRoundTripEvaluator swissRoundTripEvaluator,
            BernZurichConnectionEvaluator bernZurichConnectionEvaluator,
            TravelerGuidanceFormatEvaluator travelerGuidanceFormatEvaluator,
            LucerneMarketingTextEvaluator lucerneMarketingTextEvaluator,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            LiteralAnalyzer literalAnalyzer,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock,
            Supplier<AnalysisConfig> configSupplier
    ) {
        this.swissRoundTripEvaluator = swissRoundTripEvaluator;
        this.bernZurichConnectionEvaluator = bernZurichConnectionEvaluator;
        this.travelerGuidanceFormatEvaluator = travelerGuidanceFormatEvaluator;
        this.lucerneMarketingTextEvaluator = lucerneMarketingTextEvaluator;
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
        return switch (config.promptEvaluation()) {
            case ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP ->
                    swissRoundTripResult(namedRunLog, runLog, responses, config, literalAnalysis);
            case FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION -> factualResult(namedRunLog, runLog, responses, config, literalAnalysis);
            case LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE -> literalFormatResult(
                    namedRunLog, runLog, responses, config, literalAnalysis);
            case CREATIVE_GENERATIVE_LUCERNE_MARKETING -> creativeResult(namedRunLog, runLog, responses, config, literalAnalysis);
        };
    }

    private AnalysisResult swissRoundTripResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        SwissRoundTripEvaluation swissRoundTripEvaluation = swissRoundTripEvaluator.analyze(
                responses,
                config.swissRoundTrip(),
                clusters -> new SyntacticAnalysis(swissRoundTripSyntacticClusters(clusters, responses, config))
        );
        return result(namedRunLog, runLog, config, swissRoundTripEvaluation, null, null, null, literalAnalysis);
    }

    private AnalysisResult factualResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        BernZurichConnectionEvaluation bernZurichConnectionEvaluation = bernZurichConnectionEvaluator.analyze(
                responses,
                config.bernZurichConnection(),
                successIndices -> new SyntacticAnalysis(syntacticClusters(0, successIndices, responses, config))
        );
        return result(namedRunLog, runLog, config, null, bernZurichConnectionEvaluation, null, null, literalAnalysis);
    }

    private AnalysisResult literalFormatResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        TravelerGuidanceFormatEvaluation travelerGuidanceFormatEvaluation =
                travelerGuidanceFormatEvaluator.analyze(responses, config.travelerGuidanceFormat());
        return result(namedRunLog, runLog, config, null, null, travelerGuidanceFormatEvaluation, null,
                literalAnalysis);
    }

    private AnalysisResult creativeResult(
            NamedRunLog namedRunLog,
            RunLog runLog,
            List<String> responses,
            AnalysisConfig config,
            LiteralAnalysis literalAnalysis
    ) {
        LucerneMarketingTextEvaluation lucerneMarketingTextEvaluation = lucerneMarketingTextEvaluator.analyze(
                responses,
                config.lucerneMarketingText(),
                successIndices -> new SyntacticAnalysis(syntacticClusters(0, successIndices, responses, config))
        );
        return result(namedRunLog, runLog, config, null, null, null, lucerneMarketingTextEvaluation, literalAnalysis);
    }

    private AnalysisResult result(
            NamedRunLog namedRunLog,
            RunLog runLog,
            AnalysisConfig config,
            SwissRoundTripEvaluation swissRoundTrip,
            BernZurichConnectionEvaluation bernZurichConnection,
            TravelerGuidanceFormatEvaluation travelerGuidanceFormat,
            LucerneMarketingTextEvaluation lucerneMarketingText,
            LiteralAnalysis literal
    ) {
        return new AnalysisResult(
                namedRunLog.filename(),
                runClock.now(),
                config,
                runInfo(runLog),
                swissRoundTrip,
                bernZurichConnection,
                travelerGuidanceFormat,
                lucerneMarketingText,
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

    private List<SyntacticCluster> swissRoundTripSyntacticClusters(
            List<SwissRoundTripCluster> swissRoundTripClusters,
            List<String> responses,
            AnalysisConfig config
    ) {
        return swissRoundTripClusters.stream()
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
