package ch.thp.mas.llm.variance;

import ch.thp.mas.llm.variance.analyze.AnalyzeCommand;
import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.plan.PlanBatchResolver;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
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
    private final PlanBatchResolver planBatchResolver;
    private final PlanRunner planRunner;

    public LlmVarianceApplication(
            ApplicationArguments appArgs,
            AnalyzeCommand analyzeCommand,
            PlanBatchResolver planBatchResolver,
            PlanRunner planRunner
    ) {
        this.appArgs = appArgs;
        this.analyzeCommand = analyzeCommand;
        this.planBatchResolver = planBatchResolver;
        this.planRunner = planRunner;
    }

    public static void main(String[] args) {
        SpringApplication.run(LlmVarianceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (appArgs.containsOption("analyze")) {
            analyzeCommand.run(appArgs);
            return;
        }

        List<ResolvedPlan> plans = planBatchResolver.resolve(appArgs);
        for (ResolvedPlan plan : plans) {
            planRunner.run(plan);
        }
    }
}
