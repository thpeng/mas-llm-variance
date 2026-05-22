package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.plan.AnalysisConfigMapper;
import ch.thp.mas.llm.variance.plan.LoadedPlan;
import ch.thp.mas.llm.variance.plan.PlanLoader;
import ch.thp.mas.llm.variance.run.ExecutionEnvironmentLog;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class AnalyzeCommand {

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
        return run(appArgs, null);
    }

    public List<Path> run(ApplicationArguments appArgs, ExecutionEnvironmentLog environment) {
        if (appArgs.containsOption("run") || appArgs.containsOption("plan") || appArgs.containsOption("plans")) {
            throw new AnalysisException("Analyze mode derives the plan from each run log planName; use only --analyze=<runs|runs/subfolder|run-log>.");
        }
        String selection = optionValue(appArgs, "analyze");
        List<NamedRunLog> runLogs = runLogReader.readSelection(selection);
        return runLogs.stream()
                .map(runLog -> analyze(runLog, environment))
                .toList();
    }

    private Path analyze(NamedRunLog runLog, ExecutionEnvironmentLog environment) {
        LoadedPlan plan = planLoader.load(planSelection(runLog));
        if (!runLog.runLog().planName().equals(plan.name())) {
            throw new AnalysisException("Run log " + runLog.filename()
                    + " does not match plan " + plan.filename() + ".");
        }
        AnalysisConfig config = analysisConfigMapper.map(plan);
        return analysisWriter.write(runLog.filename(), analyzer.analyze(runLog, config, environment));
    }

    private String planSelection(NamedRunLog runLog) {
        Path parent = Path.of(runLog.filename().replace('\\', '/')).getParent();
        return parent == null
                ? runLog.runLog().planName()
                : parent.toString().replace('\\', '/') + "/" + runLog.runLog().planName();
    }

    private String optionValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new AnalysisException("--" + name + " must specify a value.");
        }
        return values.getFirst();
    }
}
