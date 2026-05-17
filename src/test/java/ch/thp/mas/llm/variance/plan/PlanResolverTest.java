package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class PlanResolverTest {

    private final PlanResolver resolver = new PlanResolver();

    @Test
    void usesYamlValuesWithoutOverrides() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args());

        assertThat(resolved.inferenceProvider()).isEqualTo(InferenceProvider.ANTHROPIC);
        assertThat(resolved.model()).isEqualTo("claude-test");
        assertThat(resolved.prompt()).isEqualTo("test prompt");
        assertThat(resolved.iterations()).isEqualTo(3);
        assertThat(resolved.temperature()).isEqualTo(0.2);
    }

    @Test
    void commandLineValuesOverrideYamlValues() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args(
                "--inferenceProvider=OPENAI",
                "--model=gpt-test",
                "--prompt=override prompt",
                "--iterations=2",
                "--temperature=0.7",
                "--topP=0.8",
                "--topK=5",
                "--seed=123",
                "--reasoning=high",
                "--modelVersion=2026-05"
        ));

        assertThat(resolved.inferenceProvider()).isEqualTo(InferenceProvider.OPENAI);
        assertThat(resolved.model()).isEqualTo("gpt-test");
        assertThat(resolved.prompt()).isEqualTo("override prompt");
        assertThat(resolved.iterations()).isEqualTo(2);
        assertThat(resolved.temperature()).isEqualTo(0.7);
        assertThat(resolved.topP()).isEqualTo(0.8);
        assertThat(resolved.topK()).isEqualTo(5);
        assertThat(resolved.seed()).isEqualTo(123L);
        assertThat(resolved.reasoning()).isEqualTo("high");
        assertThat(resolved.modelVersion()).isEqualTo("2026-05");
    }

    @Test
    void defaultsReasoningToOff() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args());

        assertThat(resolved.reasoning()).isEqualTo("off");
    }

    @Test
    void rejectsUnknownReasoning() {
        assertThatThrownBy(() -> resolver.resolve(loadedPlan(), args("--reasoning=wild")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Unknown reasoning");
    }

    @Test
    void fallsBackToInferenceProviderDefaultModel() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.LMSTUDIO);
        plan.setPrompt("test prompt");

        ResolvedPlan resolved = resolver.resolve(new LoadedPlan("0003-no-model", "0003-no-model.yml", plan), args());

        assertThat(resolved.model()).isEqualTo(InferenceProvider.LMSTUDIO.defaultModel());
    }

    @Test
    void rejectsMissingPrompt() {
        YamlPlan plan = new YamlPlan();

        assertThatThrownBy(() -> resolver.resolve(new LoadedPlan("0003-bad", "0003-bad.yml", plan), args()))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Missing prompt");
    }

    @Test
    void rejectsInvalidIterations() {
        assertThatThrownBy(() -> resolver.resolve(loadedPlan(), args("--iterations=0")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("iterations must be at least 1");
    }

    private static LoadedPlan loadedPlan() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.ANTHROPIC);
        plan.setModel("claude-test");
        plan.setPrompt("test prompt");
        plan.setIterations(3);
        plan.setTemperature(0.2);
        return new LoadedPlan("0001-test", "0001-test.yml", plan);
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
