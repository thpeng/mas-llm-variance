package ch.thp.mas.llm.variance;

import ch.thp.mas.llm.variance.analyze.AnalyzeCommand;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.metanalysis.MetaAnalysisCommand;
import ch.thp.mas.llm.variance.plan.PlanBatchResolver;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.ExecutionEnvironmentCollector;
import ch.thp.mas.llm.variance.run.ExecutionEnvironmentLog;
import ch.thp.mas.llm.variance.run.PlanRunner;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LlmVarianceApplication implements CommandLineRunner {

    private final ApplicationArguments appArgs;
    private final AnalyzeCommand analyzeCommand;
    private final MetaAnalysisCommand metaAnalysisCommand;
    private final PlanBatchResolver planBatchResolver;
    private final PlanRunner planRunner;
    private final ExecutionEnvironmentCollector environmentCollector;

    public LlmVarianceApplication(
            ApplicationArguments appArgs,
            AnalyzeCommand analyzeCommand,
            MetaAnalysisCommand metaAnalysisCommand,
            PlanBatchResolver planBatchResolver,
            PlanRunner planRunner,
            ExecutionEnvironmentCollector environmentCollector
    ) {
        this.appArgs = appArgs;
        this.analyzeCommand = analyzeCommand;
        this.metaAnalysisCommand = metaAnalysisCommand;
        this.planBatchResolver = planBatchResolver;
        this.planRunner = planRunner;
        this.environmentCollector = environmentCollector;
    }

    public static void main(String[] args) {
        SpringApplication.run(LlmVarianceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ExecutionEnvironmentLog environment = environmentCollector.snapshot();
        if (appArgs.containsOption("analyze")) {
            analyzeCommand.run(appArgs, environment);
            return;
        }
        if (appArgs.containsOption("metanalysis")) {
            metaAnalysisCommand.run(appArgs);
            return;
        }
        if (!appArgs.containsOption("run")) {
            throw new AnalysisException("Missing command. Use --run=<plans|plans/subfolder|plan>, "
                    + "--analyze=<runs|runs/subfolder|run-log> or "
                    + "--metanalysis=<analysis|analysis/subfolder|analysis-file>.");
        }

        List<ResolvedPlan> plans = planBatchResolver.resolve(appArgs);
        for (ResolvedPlan plan : plans) {
            planRunner.run(plan, environment);
        }
    }
}
