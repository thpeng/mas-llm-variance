package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class PlanBatchResolverTest {

    @TempDir
    Path plansDir;

    @Test
    void resolvesSinglePlan() throws Exception {
        writePlan("0001-test.yml");
        PlanBatchResolver resolver = resolver();

        List<ResolvedPlan> plans = resolver.resolve(args("--run=0001-test"));

        assertThat(plans).extracting(ResolvedPlan::name).containsExactly("0001-test");
    }

    @Test
    void resolvesAllPlansInRootFolder() throws Exception {
        writePlan("0002-second.yml");
        writePlan("0001-first.yml");
        PlanBatchResolver resolver = resolver();

        List<ResolvedPlan> plans = resolver.resolve(args("--run=plans"));

        assertThat(plans).extracting(ResolvedPlan::name)
                .containsExactly("0001-first", "0002-second");
    }

    @Test
    void resolvesPlansInSubfolder() throws Exception {
        writePlan("openai/0001-openai.yml");
        writePlan("anthropic/0002-anthropic.yml");
        PlanBatchResolver resolver = resolver();

        List<ResolvedPlan> plans = resolver.resolve(args("--run=plans/openai"));

        assertThat(plans).extracting(ResolvedPlan::name)
                .containsExactly("0001-openai");
    }

    @Test
    void rejectsAmbiguousPlanFileSelection() throws Exception {
        writePlan("openai/0001-duplicate.yml");
        writePlan("google/0001-duplicate.yml");
        PlanBatchResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.resolve(args("--run=0001-duplicate")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Ambiguous plan selection");
    }

    @Test
    void rejectsAmbiguousFolderSelection() throws Exception {
        writePlan("provider/openai/0001-first.yml");
        writePlan("archive/openai/0002-second.yml");
        PlanBatchResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.resolve(args("--run=openai")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Ambiguous plan folder selection");
    }

    @Test
    void rejectsOldPlanCommand() {
        assertThatThrownBy(() -> resolver().resolve(args("--plan=0001-test")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("renamed");
    }

    @Test
    void rejectsMissingRunSelection() {
        assertThatThrownBy(() -> resolver().resolve(args("--iterations=1")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Missing run selection");
    }

    private PlanBatchResolver resolver() {
        return new PlanBatchResolver(new PlanLoader(plansDir), new PlanResolver());
    }

    private void writePlan(String relativePath) throws Exception {
        Path path = plansDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                inferenceProvider: OPENAI
                model: gpt-test
                analysis:
                  promptEvaluation: CREATIVE_GENERATIVE_LUCERNE_MARKETING
                run:
                  prompt: "prompt"
                  iterations: 1
                  temperature: 0.0
                  topP: 1.0
                  topK: 1
                  seed: 1
                  reasoning: off
                """);
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
