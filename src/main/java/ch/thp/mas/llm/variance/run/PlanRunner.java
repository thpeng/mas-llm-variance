package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LlmClientFactory;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.LlmResponse;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlanRunner {

    private final InferenceSessionFactory sessionFactory;
    private final RunClock runClock;
    private final RunLogWriter runLogWriter;
    private final RandomGenerator randomGenerator;

    @Autowired
    public PlanRunner(InferenceSessionFactory sessionFactory, RunClock runClock, RunLogWriter runLogWriter) {
        this(sessionFactory, runClock, runLogWriter, RandomGenerator.getDefault());
    }

    PlanRunner(LlmClientFactory clientFactory, RunClock runClock, RunLogWriter runLogWriter) {
        this((InferenceSessionFactory) plan -> new NoopInferenceSession(clientFactory.create(plan.inferenceProvider())),
                runClock,
                runLogWriter,
                RandomGenerator.getDefault());
    }

    PlanRunner(
            InferenceSessionFactory sessionFactory,
            RunClock runClock,
            RunLogWriter runLogWriter,
            RandomGenerator randomGenerator
    ) {
        this.sessionFactory = sessionFactory;
        this.runClock = runClock;
        this.runLogWriter = runLogWriter;
        this.randomGenerator = randomGenerator;
    }

    public RunLog run(ResolvedPlan plan) throws Exception {
        System.out.println("=== Running plan: " + plan.name() + " ===");

        OffsetDateTime runStartedAt = runClock.now();

        List<RunLogEntry> repetitions = new ArrayList<>();
        InferenceSession session = sessionFactory.open(plan);
        ModelInstanceLog modelInstance = session.modelInstance();
        try {
            for (int i = 0; i < plan.iterations(); i++) {
                Long requestSeed = seedForRepetition(plan);
                LlmRequestConfig config = new LlmRequestConfig(
                        plan.model(),
                        plan.temperature(),
                        plan.topP(),
                        plan.topK(),
                        requestSeed,
                        plan.reasoning(),
                        plan.sendReasoning(),
                        plan.reasoningProviderValue()
                );
                OffsetDateTime startedAt = runClock.now();
                LlmResponse response = session.client().call(plan.prompt(), config);
                OffsetDateTime endedAt = runClock.now();
                repetitions.add(new RunLogEntry(i + 1, startedAt, endedAt, requestSeed, response.text(),
                        response.tokenUsage()));
                System.out.println(response.text() + ", ");
            }
        } catch (Exception e) {
            closeAfterFailure(session, e);
            throw e;
        }
        session.close();

        RunLog runLog = new RunLog(
                plan.name(),
                runStartedAt,
                runClock.now(),
                plan.inferenceProvider(),
                plan.model(),
                plan.modelVersion(),
                modelInstance,
                plan.iterations(),
                new RunConfigLog(plan.temperature(), plan.topP(), plan.topK(), plan.seed(), plan.seedSetting(),
                        plan.reasoning(), plan.sendReasoning(), plan.reasoningProviderValue()),
                plan.prompt(),
                List.copyOf(repetitions)
        );
        runLogWriter.write(runLog);
        return runLog;
    }

    private static void closeAfterFailure(InferenceSession session, Exception failure) {
        try {
            session.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private Long seedForRepetition(ResolvedPlan plan) {
        if (!"RANDOM".equals(plan.seedSetting())) {
            return plan.seed();
        }
        return randomGenerator.nextLong(0, Long.MAX_VALUE);
    }
}
