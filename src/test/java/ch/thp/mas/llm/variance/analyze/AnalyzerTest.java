package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyzerTest {

    @Test
    void rejectsRunWithoutResponses() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        RunLog empty = new RunLog(
                "0001-empty",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                0,
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "prompt",
                List.of()
        );

        assertThatThrownBy(() -> TestAnalyzerFactory.create(AnalysisConfig.defaults())
                .analyze(new NamedRunLog("empty.json", empty)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no successful responses");
    }

    static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-02T11:00:00+02:00");
        }
    }
}
