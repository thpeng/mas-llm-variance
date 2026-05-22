package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyzerTest {

    @Test
    void analyzesRunWithoutSuccessfulResponses() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        RunLog empty = new RunLog(
                "0001-serving-error",
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                1,
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "prompt",
                List.of(RunLogEntry.servingError(
                        1,
                        now,
                        now,
                        null,
                        "https://example.test",
                        null,
                        "{}",
                        500,
                        null,
                        "{\"error\":\"temporary\"}",
                        500,
                        "temporary serving error",
                        "{\"error\":\"temporary\"}"
                ))
        );

        AnalysisResult result = TestAnalyzerFactory.create(AnalysisConfig.defaults())
                .analyze(new NamedRunLog("serving-error.json", empty));

        assertThat(result.run().servingErrorCount()).isEqualTo(1);
        assertThat(result.literal().responseCount()).isZero();
        assertThat(result.swissRoundTrip().responseCount()).isZero();
        assertThat(result.swissRoundTrip().outliers()).isEmpty();
    }

    static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-02T11:00:00+02:00");
        }
    }
}
