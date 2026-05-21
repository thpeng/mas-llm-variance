package ch.thp.mas.llm.variance.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.LlmResponse;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.RequestTrace;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.client.ServingException;
import ch.thp.mas.llm.variance.client.TokenUsage;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class PlanRunnerTest {

    @Test
    void callsClientForEveryIterationAndWritesRunLog() throws Exception {
        RecordingClient client = new RecordingClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner((ch.thp.mas.llm.variance.client.LlmClientFactory) inferenceProvider -> client,
                new FixedRunClock(),
                writer);
        ResolvedPlan plan = new ResolvedPlan(
                "0001-test",
                InferenceProvider.OPENAI,
                "gpt-test",
                "hello",
                3,
                0.1,
                0.9,
                4,
                123L,
                Reasoning.OFF,
                null,
                null
        );

        RunLog runLog = runner.run(plan);

        assertThat(writer.logs).containsExactly(runLog);
        assertThat(writer.sourcePlanPaths).containsExactly("");
        assertThat(runLog.planName()).isEqualTo("0001-test");
        assertThat(runLog.modelVersion()).isNull();
        assertThat(runLog.config().seed()).isEqualTo(123L);
        assertThat(runLog.config().seedSetting()).isEqualTo("123");
        assertThat(runLog.config().reasoning()).isEqualTo(Reasoning.OFF);
        assertThat(runLog.repetitions()).hasSize(3);
        assertThat(runLog.repetitions()).extracting(RunLogEntry::seed)
                .containsExactly(123L, 123L, 123L);
        assertThat(runLog.repetitions()).extracting(RunLogEntry::response)
                .containsExactly("answer-1", "answer-2", "answer-3");
        assertThat(runLog.repetitions().getFirst().requestUrl()).isEqualTo("https://example.test/chat");
        assertThat(runLog.repetitions().getFirst().requestHeaders())
                .containsEntry("Content-Type", List.of("application/json"));
        assertThat(runLog.repetitions().getFirst().requestHeaders())
                .doesNotContainKey("Authorization");
        assertThat(runLog.repetitions().getFirst().requestBody()).contains("prompt");
        assertThat(runLog.repetitions().getFirst().responseStatusCode()).isEqualTo(200);
        assertThat(runLog.repetitions().getFirst().responseBody()).contains("raw-provider-response");
        assertThat(runLog.repetitions().getFirst().tokenUsage())
                .isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(client.prompts).containsExactly("hello", "hello", "hello");
        assertThat(client.configs).allSatisfy(config -> {
            assertThat(config.model()).isEqualTo("gpt-test");
            assertThat(config.temperature()).isEqualTo(0.1);
            assertThat(config.topP()).isEqualTo(0.9);
            assertThat(config.topK()).isEqualTo(4);
            assertThat(config.seed()).isEqualTo(123L);
            assertThat(config.reasoning()).isEqualTo(Reasoning.OFF);
        });
    }

    @Test
    void generatesAndLogsConcreteSeedForEachRandomSeedRepetition() throws Exception {
        RecordingClient client = new RecordingClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(
                (InferenceSessionFactory) plan -> new RecordingSession(client),
                new FixedRunClock(),
                writer,
                new FixedRandomGenerator(41L)
        );

        RunLog runLog = runner.run(new ResolvedPlan(
                "0001-test",
                InferenceProvider.OPENAI,
                "gpt-test",
                "hello",
                3,
                0.1,
                0.9,
                4,
                null,
                "RANDOM",
                Reasoning.OFF,
                true,
                null,
                null,
                null
        ));

        assertThat(runLog.config().seed()).isNull();
        assertThat(runLog.config().seedSetting()).isEqualTo("RANDOM");
        assertThat(runLog.repetitions()).extracting(RunLogEntry::seed)
                .containsExactly(42L, 43L, 44L);
        assertThat(client.configs).extracting(LlmRequestConfig::seed)
                .containsExactly(42L, 43L, 44L);
    }

    @Test
    void rejectsRandomSeedForLmStudio() {
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(
                (InferenceSessionFactory) plan -> new RecordingSession(new RecordingClient()),
                new FixedRunClock(),
                writer,
                new FixedRandomGenerator(41L)
        );

        assertThatThrownBy(() -> runner.run(new ResolvedPlan(
                "0001-test",
                InferenceProvider.LMSTUDIO,
                "model-a",
                "hello",
                1,
                null,
                null,
                null,
                null,
                "RANDOM",
                Reasoning.OFF,
                true,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed: RANDOM");
        assertThat(writer.logs).isEmpty();
    }

    @Test
    void logsFixedSeedForLmStudioButDoesNotSendItToChatRequest() throws Exception {
        RecordingClient client = new RecordingClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(
                (InferenceSessionFactory) plan -> new RecordingSession(client),
                new FixedRunClock(),
                writer
        );

        RunLog runLog = runner.run(new ResolvedPlan(
                "0001-test",
                InferenceProvider.LMSTUDIO,
                "model-a",
                "hello",
                1,
                null,
                null,
                null,
                123L,
                "123",
                Reasoning.OFF,
                true,
                null,
                null,
                null,
                null
        ));

        assertThat(runLog.repetitions()).extracting(RunLogEntry::seed).containsExactly(123L);
        assertThat(client.configs).extracting(LlmRequestConfig::seed).containsExactly((Long) null);
    }

    @Test
    void doesNotWriteLogWhenClientCallFails() {
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(
                (ch.thp.mas.llm.variance.client.LlmClientFactory) inferenceProvider -> new FailingClient(),
                new FixedRunClock(),
                writer);
        ResolvedPlan plan = new ResolvedPlan(
                "0001-test",
                InferenceProvider.OPENAI,
                "gpt-test",
                "hello",
                1,
                null,
                null,
                null,
                null,
                null,
                Reasoning.OFF,
                true,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> runner.run(plan))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
        assertThat(writer.logs).isEmpty();
    }

    @Test
    void recordsServingErrorsAndContinuesRun() throws Exception {
        ServingErrorThenSuccessClient client = new ServingErrorThenSuccessClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(
                (ch.thp.mas.llm.variance.client.LlmClientFactory) inferenceProvider -> client,
                new FixedRunClock(),
                writer);

        RunLog runLog = runner.run(plan(2));

        assertThat(runLog.repetitions()).hasSize(2);
        assertThat(runLog.errors().servingErrorCount()).isEqualTo(1);
        assertThat(runLog.repetitions().getFirst().status()).isEqualTo(RunLogEntryStatus.SERVING_ERROR);
        assertThat(runLog.repetitions().getFirst().errorStatusCode()).isEqualTo(503);
        assertThat(runLog.repetitions().getFirst().requestUrl()).isEqualTo("https://example.test/chat");
        assertThat(runLog.repetitions().getFirst().requestBody()).contains("prompt");
        assertThat(runLog.repetitions().getFirst().responseStatusCode()).isEqualTo(503);
        assertThat(runLog.repetitions().getFirst().responseBody()).contains("busy");
        assertThat(runLog.repetitions().getFirst().response()).isNull();
        assertThat(runLog.repetitions().get(1).status()).isEqualTo(RunLogEntryStatus.SUCCESS);
        assertThat(runLog.repetitions().get(1).response()).isEqualTo("answer-2");
        assertThat(writer.logs).containsExactly(runLog);
    }

    @Test
    void closesSessionBeforeWritingRunLogAndIncludesModelInstance() throws Exception {
        RecordingClient client = new RecordingClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        RecordingSession session = new RecordingSession(client);
        PlanRunner runner = new PlanRunner((InferenceSessionFactory) plan -> session, new FixedRunClock(), writer);

        RunLog runLog = runner.run(plan(1));

        assertThat(session.closed).isTrue();
        assertThat(writer.logs).containsExactly(runLog);
        assertThat(runLog.modelInstance()).isEqualTo(session.modelInstance());
    }

    @Test
    void closesSessionAndDoesNotWriteLogWhenInvocationFails() {
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        RecordingSession session = new RecordingSession(new FailingClient());
        PlanRunner runner = new PlanRunner((InferenceSessionFactory) plan -> session, new FixedRunClock(), writer);

        assertThatThrownBy(() -> runner.run(plan(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        assertThat(session.closed).isTrue();
        assertThat(writer.logs).isEmpty();
    }

    @Test
    void doesNotWriteLogWhenSessionCloseFailsAfterSuccessfulInvocation() {
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        RecordingSession session = new RecordingSession(new RecordingClient());
        session.closeFailure = new IllegalStateException("cleanup failed");
        PlanRunner runner = new PlanRunner((InferenceSessionFactory) plan -> session, new FixedRunClock(), writer);

        assertThatThrownBy(() -> runner.run(plan(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup failed");

        assertThat(writer.logs).isEmpty();
    }

    private static class RecordingClient implements LlmClient {

        protected final List<String> prompts = new ArrayList<>();
        protected final List<LlmRequestConfig> configs = new ArrayList<>();

        @Override
        public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
            prompts.add(prompt);
            configs.add(config);
            return new LlmResponse(
                    "answer-" + prompts.size(),
                    new TokenUsage(10L, 5L, 15L),
                    null,
                    RequestTrace.of("https://example.test/chat", Map.of(
                            "Authorization", List.of("Bearer secret"),
                            "Content-Type", List.of("application/json")
                    ), "{\"input\":\"prompt\"}", 200, Map.of("Content-Type", List.of("application/json")),
                            "{\"output\":\"raw-provider-response\"}")
            );
        }
    }

    private static class FailingClient implements LlmClient {

        @Override
        public LlmResponse call(String prompt, LlmRequestConfig config) {
            throw new IllegalStateException("boom");
        }
    }

    private static class ServingErrorThenSuccessClient extends RecordingClient {

        @Override
        public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
            if (prompts.isEmpty()) {
                prompts.add(prompt);
                configs.add(config);
                throw new ServingException("provider unavailable", 503, "{\"error\":\"busy\"}",
                        RequestTrace.of("https://example.test/chat", Map.of(
                                "Authorization", List.of("Bearer secret"),
                                "Content-Type", List.of("application/json")
                        ), "{\"input\":\"prompt\"}", 503, Map.of("Content-Type", List.of("application/json")),
                                "{\"error\":\"busy\"}"));
            }
            return super.call(prompt, config);
        }
    }

    private static ResolvedPlan plan(int iterations) {
        return new ResolvedPlan(
                "0001-test",
                InferenceProvider.LMSTUDIO,
                "gpt-test",
                "hello",
                iterations,
                null,
                null,
                null,
                null,
                null,
                Reasoning.OFF,
                true,
                null,
                null,
                null
        );
    }

    private static class RecordingSession implements InferenceSession {

        private final LlmClient client;
        private final ModelInstanceLog modelInstance = new ModelInstanceLog(
                "instance-a",
                true,
                new LmStudioLoadConfigLog(null, null, null, null, null),
                JsonNodeFactory.instance.objectNode().put("status", "loaded")
        );
        private boolean closed;
        private Exception closeFailure;

        RecordingSession(LlmClient client) {
            this.client = client;
        }

        @Override
        public LlmClient client() {
            return client;
        }

        @Override
        public ModelInstanceLog modelInstance() {
            return modelInstance;
        }

        @Override
        public void close() throws Exception {
            closed = true;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static class FixedRunClock implements RunClock {

        private final Queue<OffsetDateTime> timestamps = new ArrayDeque<>(List.of(
                OffsetDateTime.parse("2026-05-02T10:00:00+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:01+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:02+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:03+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:04+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:05+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:06+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:07+02:00")
        ));

        @Override
        public OffsetDateTime now() {
            return timestamps.remove();
        }
    }

    private static class RecordingRunLogWriter extends RunLogWriter {

        private final List<RunLog> logs = new ArrayList<>();
        private final List<String> sourcePlanPaths = new ArrayList<>();

        RecordingRunLogWriter() {
            super(new RunFileNameFactory());
        }

        @Override
        public java.nio.file.Path write(RunLog runLog) {
            return write(runLog, "");
        }

        @Override
        public java.nio.file.Path write(RunLog runLog, String sourcePlanPath) {
            logs.add(runLog);
            sourcePlanPaths.add(sourcePlanPath == null ? "" : sourcePlanPath);
            return java.nio.file.Path.of("ignored");
        }
    }

    private static class FixedRandomGenerator implements RandomGenerator {

        private long value;

        FixedRandomGenerator(long initialValue) {
            this.value = initialValue;
        }

        @Override
        public long nextLong(long origin, long bound) {
            return ++value;
        }

        @Override
        public long nextLong() {
            return nextLong(0, Long.MAX_VALUE);
        }

        @Override
        public int nextInt() {
            return Math.toIntExact(nextLong());
        }
    }
}
