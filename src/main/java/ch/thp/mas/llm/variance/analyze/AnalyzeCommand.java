package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.plan.AnalysisConfigMapper;
import ch.thp.mas.llm.variance.plan.LoadedPlan;
import ch.thp.mas.llm.variance.plan.PlanLoader;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class AnalyzeCommand {

    private static final String ALL = "ALL";

    private final RunLogReader runLogReader;
    private final PlanLoader planLoader;
    private final AnalysisConfigMapper analysisConfigMapper;
    private final Analyzer analyzer;
    private final AnalysisWriter analysisWriter;

    public AnalyzeCommand(
            RunLogReader runLogReader,
            PlanLoader planLoader,
            AnalysisConfigMapper analysisConfigMapper,
            Analyzer analyzer,
            AnalysisWriter analysisWriter
    ) {
        this.runLogReader = runLogReader;
        this.planLoader = planLoader;
        this.analysisConfigMapper = analysisConfigMapper;
        this.analyzer = analyzer;
        this.analysisWriter = analysisWriter;
    }

    public List<Path> run(ApplicationArguments appArgs) {
        if (appArgs.containsOption("plan") || appArgs.containsOption("plans")) {
            throw new AnalysisException("Analyze mode derives the plan from each run log planName; do not pass --plan or --plans.");
        }
        String selection = optionValue(appArgs, "analyze");
        List<NamedRunLog> runLogs = ALL.equalsIgnoreCase(selection)
                ? runLogReader.readAll()
                : List.of(runLogReader.read(selection));
        return runLogs.stream()
                .map(this::analyze)
                .toList();
    }

    private Path analyze(NamedRunLog runLog) {
        LoadedPlan plan = planLoader.load(runLog.runLog().planName());
        if (!runLog.runLog().planName().equals(plan.name())) {
            throw new AnalysisException("Run log " + runLog.filename()
                    + " does not match plan " + plan.filename() + ".");
        }
        AnalysisConfig config = analysisConfigMapper.map(plan);
        return analysisWriter.write(runLog.filename(), analyzer.analyze(runLog, config));
    }

    private String optionValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new AnalysisException("--" + name + " must specify a value.");
        }
        return values.getFirst();
    }
}
