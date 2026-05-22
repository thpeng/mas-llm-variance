package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.plan.AnalysisConfigMapper;
import ch.thp.mas.llm.variance.plan.LoadedPlan;
import ch.thp.mas.llm.variance.plan.PlanLoader;
import ch.thp.mas.llm.variance.plan.YamlAnalysisConfig;
import ch.thp.mas.llm.variance.plan.YamlPlan;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

class AnalyzeCommandTest {

    private final RunLogReader runLogReader = mock(RunLogReader.class);
    private final PlanLoader planLoader = mock(PlanLoader.class);
    private final Analyzer analyzer = mock(Analyzer.class);
    private final AnalysisWriter analysisWriter = mock(AnalysisWriter.class);
    private final AnalyzeCommand command = new AnalyzeCommand(
            runLogReader,
            planLoader,
            new AnalysisConfigMapper(),
            analyzer,
            analysisWriter
    );

    @Test
    void analyzesRunLogWithPlanAnalysisConfig() {
        NamedRunLog runLog = new NamedRunLog("run.json", runLog("0001-test"));
        LoadedPlan plan = loadedPlan("0001-test", PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP);
        AnalysisResult result = mock(AnalysisResult.class);
        when(runLogReader.readSelection("run.json")).thenReturn(List.of(runLog));
        when(planLoader.load("0001-test")).thenReturn(plan);
        ArgumentCaptor<AnalysisConfig> configCaptor = ArgumentCaptor.forClass(AnalysisConfig.class);
        when(analyzer.analyze(eq(runLog), configCaptor.capture(), isNull())).thenReturn(result);
        when(analysisWriter.write("run.json", result)).thenReturn(Path.of("analysis.json"));

        command.run(args("--analyze=run.json"));

        verify(analysisWriter).write("run.json", result);
        org.assertj.core.api.Assertions.assertThat(configCaptor.getValue().promptEvaluation())
                .isEqualTo(PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP);
    }

    @Test
    void loadsPlanByRunLogPlanName() {
        when(runLogReader.readSelection("run.json")).thenReturn(List.of(new NamedRunLog("run.json", runLog("0001-test"))));
        when(planLoader.load("0001-test")).thenReturn(loadedPlan("0001-test", PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING));
        AnalysisResult result = mock(AnalysisResult.class);
        when(analyzer.analyze(
                eq(new NamedRunLog("run.json", runLog("0001-test"))),
                org.mockito.ArgumentMatchers.any(),
                isNull()
        ))
                .thenReturn(result);

        command.run(args("--analyze=run.json"));

        verify(planLoader).load("0001-test");
    }

    @Test
    void loadsPlanFromSameRelativeFolderAsRunLog() {
        NamedRunLog runLog = new NamedRunLog("test/run.json", runLog("0001-test"));
        when(runLogReader.readSelection("runs/test")).thenReturn(List.of(runLog));
        when(planLoader.load("test/0001-test")).thenReturn(loadedPlan("0001-test", PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING));
        AnalysisResult result = mock(AnalysisResult.class);
        when(analyzer.analyze(eq(runLog), org.mockito.ArgumentMatchers.any(), isNull())).thenReturn(result);

        command.run(args("--analyze=runs/test"));

        verify(planLoader).load("test/0001-test");
    }

    @Test
    void rejectsPlanWithoutAnalysisBlock() {
        when(runLogReader.readSelection("run.json")).thenReturn(List.of(new NamedRunLog("run.json", runLog("0001-test"))));
        when(planLoader.load("0001-test")).thenReturn(new LoadedPlan("0001-test", "0001-test.yml", new YamlPlan()));

        assertThatThrownBy(() -> command.run(args("--analyze=run.json")))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("Missing analysis block");
    }

    @Test
    void rejectsExplicitPlanSelectionInAnalyzeMode() {
        assertThatThrownBy(() -> command.run(args("--analyze=run.json", "--run=0001-test")))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("derives the plan");
    }

    private static LoadedPlan loadedPlan(String name, PromptEvaluation promptEvaluation) {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setPromptEvaluation(promptEvaluation);
        if (promptEvaluation == PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP) {
            YamlAnalysisConfig.SwissRoundTrip swissRoundTrip = new YamlAnalysisConfig.SwissRoundTrip();
            swissRoundTrip.setExpectedStationCount(5);
            swissRoundTrip.setLanguage(PromptLanguage.DE);
            analysis.setSwissRoundTrip(swissRoundTrip);
        }
        plan.setAnalysis(analysis);
        return new LoadedPlan(name, name + ".yml", plan);
    }

    private static RunLog runLog(String planName) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new RunLog(
                planName,
                now,
                now,
                InferenceProvider.OPENAI,
                "gpt-test",
                null,
                null,
                1,
                new RunConfigLog(0.0, null, null, null, Reasoning.OFF),
                "prompt",
                List.of(new RunLogEntry(1, now, now, "response", null))
        );
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
