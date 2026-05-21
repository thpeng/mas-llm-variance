package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LlmClientFactory;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.LlmResponse;
import ch.thp.mas.llm.variance.client.RequestTrace;
import ch.thp.mas.llm.variance.client.ServingException;
import ch.thp.mas.llm.variance.client.InferenceProvider;
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
        ResolvedPlan runtimePlan = runtimePlan(plan);
        System.out.println("=== Running plan: " + runtimePlan.name() + " ===");

        OffsetDateTime runStartedAt = runClock.now();

        List<RunLogEntry> repetitions = new ArrayList<>();
        InferenceSession session = sessionFactory.open(runtimePlan);
        try {
            for (int i = 0; i < runtimePlan.iterations(); i++) {
                Long requestSeed = seedForRepetition(runtimePlan);
                LlmRequestConfig config = new LlmRequestConfig(
                        runtimePlan.model(),
                        runtimePlan.temperature(),
                        runtimePlan.topP(),
                        runtimePlan.topK(),
                        runtimePlan.inferenceProvider() == InferenceProvider.LMSTUDIO ? null : requestSeed,
                        runtimePlan.reasoning(),
                        runtimePlan.sendReasoning(),
                        runtimePlan.reasoningProviderValue()
                );
                OffsetDateTime startedAt = runClock.now();
                try {
                    LlmResponse response = session.client().call(runtimePlan.prompt(), config);
                    OffsetDateTime endedAt = runClock.now();
                    RequestTrace requestTrace = response.requestTrace();
                    repetitions.add(new RunLogEntry(i + 1, startedAt, endedAt, requestSeed,
                            requestTrace == null ? null : requestTrace.url(),
                            requestTrace == null ? null : requestTrace.headers(),
                            requestTrace == null ? null : requestTrace.body(),
                            requestTrace == null ? null : requestTrace.responseStatusCode(),
                            requestTrace == null ? null : requestTrace.responseHeaders(),
                            requestTrace == null ? null : requestTrace.responseBody(),
                            response.text(),
                            response.tokenUsage()));
                    System.out.println(response.text() + ", ");
                } catch (ServingException e) {
                    if (!e.isServingError()) {
                        throw e;
                    }
                    OffsetDateTime endedAt = runClock.now();
                    RequestTrace requestTrace = e.requestTrace();
                    repetitions.add(RunLogEntry.servingError(
                            i + 1,
                            startedAt,
                            endedAt,
                            requestSeed,
                            requestTrace == null ? null : requestTrace.url(),
                            requestTrace == null ? null : requestTrace.headers(),
                            requestTrace == null ? null : requestTrace.body(),
                            requestTrace == null ? null : requestTrace.responseStatusCode(),
                            requestTrace == null ? null : requestTrace.responseHeaders(),
                            requestTrace == null ? null : requestTrace.responseBody(),
                            e.statusCode(),
                            e.getMessage(),
                            e.responseBody()
                    ));
                    System.out.println("Serving error " + e.statusCode() + " in repetition " + (i + 1)
                            + ", continuing run.");
                }
            }
        } catch (Exception e) {
            closeAfterFailure(session, e);
            throw e;
        }
        session.close();
        ModelInstanceLog modelInstance = session.modelInstance();

        RunLog runLog = new RunLog(
                runtimePlan.name(),
                runStartedAt,
                runClock.now(),
                runtimePlan.inferenceProvider(),
                runtimePlan.model(),
                runtimePlan.modelVersion(),
                modelInstance,
                runtimePlan.iterations(),
                new RunConfigLog(runtimePlan.temperature(), runtimePlan.topP(), runtimePlan.topK(), runtimePlan.seed(), runtimePlan.seedSetting(),
                        runtimePlan.reasoning(), runtimePlan.sendReasoning(), runtimePlan.reasoningProviderValue()),
                runtimePlan.prompt(),
                List.copyOf(repetitions)
        );
        runLogWriter.write(runLog, runtimePlan.sourcePath());
        return runLog;
    }

    private ResolvedPlan runtimePlan(ResolvedPlan plan) {
        if (plan.inferenceProvider() == InferenceProvider.LMSTUDIO && "RANDOM".equals(plan.seedSetting())) {
            throw new IllegalArgumentException("LM Studio does not support seed: RANDOM because seed is applied at model load.");
        }
        return plan;
    }

    private static void closeAfterFailure(InferenceSession session, Exception failure) {
        try {
            session.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private Long seedForRepetition(ResolvedPlan plan) {
        if (plan.seedSetting() == null) {
            return null;
        }
        if (plan.inferenceProvider() == InferenceProvider.LMSTUDIO) {
            return plan.seed();
        }
        if (!"RANDOM".equals(plan.seedSetting())) {
            return plan.seed();
        }
        return randomGenerator.nextLong(0, Long.MAX_VALUE);
    }
}
