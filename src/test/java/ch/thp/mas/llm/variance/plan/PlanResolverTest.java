package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
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
        assertThat(resolved.seedSetting()).isEqualTo("123");
        assertThat(resolved.reasoning()).isEqualTo(Reasoning.HIGH);
        assertThat(resolved.sendReasoning()).isTrue();
        assertThat(resolved.reasoningProviderValue()).isNull();
        assertThat(resolved.modelVersion()).isEqualTo("2026-05");
    }

    @Test
    void commandLineCanDisableSendingReasoning() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args("--sendReasoning=false"));

        assertThat(resolved.reasoning()).isEqualTo(Reasoning.OFF);
        assertThat(resolved.sendReasoning()).isFalse();
    }

    @Test
    void resolvesReasoningToEnum() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args());

        assertThat(resolved.reasoning()).isEqualTo(Reasoning.OFF);
        assertThat(resolved.sendReasoning()).isTrue();
    }

    @Test
    void acceptsXhighReasoning() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args("--reasoning=xhigh"));

        assertThat(resolved.reasoning()).isEqualTo(Reasoning.XHIGH);
    }

    @Test
    void acceptsRandomSeed() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args("--seed=random"));

        assertThat(resolved.seed()).isNull();
        assertThat(resolved.seedSetting()).isEqualTo("RANDOM");
    }

    @Test
    void acceptsReasoningProviderValue() {
        ResolvedPlan resolved = resolver.resolve(loadedPlan(), args("--reasoning=high", "--reasoningProviderValue=on"));

        assertThat(resolved.reasoning()).isEqualTo(Reasoning.HIGH);
        assertThat(resolved.reasoningProviderValue()).isEqualTo("on");
    }

    @Test
    void rejectsUnknownReasoning() {
        assertThatThrownBy(() -> resolver.resolve(loadedPlan(), args("--reasoning=wild")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Unknown reasoning");
    }

    @Test
    void rejectsOnReasoningAliasExceptQwenLmStudio() {
        assertThatThrownBy(() -> resolver.resolve(loadedPlan(), args("--reasoning=on")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("only supported for Qwen via LM Studio");
    }

    @Test
    void acceptsOnReasoningForQwenViaLmStudio() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.LMSTUDIO);
        plan.setModel("qwen/qwen3.5-9b");
        plan.setRun(run("test prompt", 3, null, null, null, null, "on"));

        ResolvedPlan resolved = resolver.resolve(new LoadedPlan("0004-qwen", "0004-qwen.yml", plan), args());

        assertThat(resolved.reasoning()).isNull();
        assertThat(resolved.sendReasoning()).isTrue();
        assertThat(resolved.reasoningProviderValue()).isEqualTo("on");
    }

    @Test
    void fallsBackToInferenceProviderDefaultModel() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.LMSTUDIO);
        plan.setRun(run("test prompt", 3, 0.2, 1.0, 1, null, "off"));

        ResolvedPlan resolved = resolver.resolve(new LoadedPlan("0003-no-model", "0003-no-model.yml", plan), args());

        assertThat(resolved.model()).isEqualTo(InferenceProvider.LMSTUDIO.defaultModel());
        assertThat(resolved.seed()).isNull();
    }

    @Test
    void rejectsSeedForLmStudio() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.LMSTUDIO);
        plan.setRun(run("test prompt", 3, 0.2, 1.0, 1, "123", "off"));

        assertThatThrownBy(() -> resolver.resolve(new LoadedPlan("0003-lmstudio", "0003-lmstudio.yml", plan), args()))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("does not support seed");
    }

    @Test
    void rejectsCommandLineSeedForLmStudio() {
        YamlPlan plan = new YamlPlan();
        plan.setInferenceProvider(InferenceProvider.LMSTUDIO);
        plan.setRun(run("test prompt", 3, 0.2, 1.0, 1, null, "off"));

        assertThatThrownBy(() -> resolver.resolve(
                new LoadedPlan("0003-lmstudio", "0003-lmstudio.yml", plan),
                args("--seed=random")
        ))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("does not support seed");
    }

    @Test
    void rejectsMissingPrompt() {
        YamlPlan plan = new YamlPlan();
        plan.setRun(run(null, 3, 0.2, 1.0, 1, "RANDOM", "off"));

        assertThatThrownBy(() -> resolver.resolve(new LoadedPlan("0003-bad", "0003-bad.yml", plan), args()))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Missing run.prompt");
    }

    @Test
    void rejectsMissingRunBlock() {
        YamlPlan plan = new YamlPlan();

        assertThatThrownBy(() -> resolver.resolve(new LoadedPlan("0003-bad", "0003-bad.yml", plan), args()))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Missing run block");
    }

    @Test
    void acceptsMissingProviderSpecificRunParameter() {
        YamlPlan plan = new YamlPlan();
        plan.setRun(run("test prompt", 3, 0.2, null, 1, "RANDOM", "off"));

        ResolvedPlan resolved = resolver.resolve(new LoadedPlan("0003-ok", "0003-ok.yml", plan), args());

        assertThat(resolved.topP()).isNull();
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
        plan.setRun(run("test prompt", 3, 0.2, 0.9, 4, "123", "off"));
        return new LoadedPlan("0001-test", "0001-test.yml", plan);
    }

    private static YamlRunConfig run(
            String prompt,
            Integer iterations,
            Double temperature,
            Double topP,
            Integer topK,
            String seed,
            String reasoning
    ) {
        YamlRunConfig run = new YamlRunConfig();
        run.setPrompt(prompt);
        run.setIterations(iterations);
        run.setTemperature(temperature);
        run.setTopP(topP);
        run.setTopK(topK);
        run.setSeed(seed);
        run.setReasoning(reasoning);
        return run;
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
