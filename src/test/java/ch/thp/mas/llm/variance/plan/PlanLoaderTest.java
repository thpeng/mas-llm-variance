package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlanLoaderTest {

    @TempDir
    Path plansDir;

    @Test
    void loadsPlanByNameWithoutSuffix() throws Exception {
        writePlan("0001-rundreise-schweiz.yml");
        PlanLoader planLoader = new PlanLoader(plansDir);

        LoadedPlan loadedPlan = planLoader.load("0001-rundreise-schweiz");

        assertThat(loadedPlan.name()).isEqualTo("0001-rundreise-schweiz");
        assertThat(loadedPlan.plan().getPrompt()).contains("Rundreise");
        assertThat(((YamlPlan) loadedPlan.plan()).getRun().getSeed()).isEqualTo("RANDOM");
        assertThat(loadedPlan.plan().getSeed()).isNull();
        assertThat(((YamlPlan) loadedPlan.plan()).getDescription())
                .contains("offene Antwort")
                .contains("Freiheitsgraden.\nDie Reiseplanung")
                .contains("Detailauswahl");
        assertThat(((YamlPlan) loadedPlan.plan()).getAnalysis().getPromptEvaluation().name())
                .isEqualTo("ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP");
        assertThat(((YamlPlan) loadedPlan.plan()).getAnalysis().getSwissRoundTrip().getExpectedStationCount()).isEqualTo(5);
        assertThat(((YamlPlan) loadedPlan.plan()).getAnalysis().getSwissRoundTrip().getLanguage().name()).isEqualTo("DE");
    }

    @Test
    void loadsPlanByNameWithSuffix() throws Exception {
        writePlan("0001-rundreise-schweiz.yml");
        PlanLoader planLoader = new PlanLoader(plansDir);

        LoadedPlan loadedPlan = planLoader.load("0001-rundreise-schweiz.yml");

        assertThat(loadedPlan.name()).isEqualTo("0001-rundreise-schweiz");
        assertThat(loadedPlan.filename()).isEqualTo("0001-rundreise-schweiz.yml");
    }

    @Test
    void loadsSelectionFolderRecursively() throws Exception {
        writePlan("gpt/0002-second.yml");
        writePlan("gpt/nested/0001-first.yml");
        writePlan("other/0003-other.yml");
        PlanLoader planLoader = new PlanLoader(plansDir);

        List<LoadedPlan> loadedPlans = planLoader.loadSelection("plans/gpt");

        assertThat(loadedPlans).extracting(LoadedPlan::name)
                .containsExactly("0001-first", "0002-second");
    }

    @Test
    void rejectsUnknownPlanName() {
        assertThatThrownBy(() -> new PlanLoader(plansDir).load("9999-missing"))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Plan selection not found");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> new PlanLoader(plansDir).load("../0001-rundreise-schweiz"))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Invalid plan selection");
    }

    @Test
    void discoversPlansInNaturalOrder() throws Exception {
        writePlan("0002-hauptstadt-798.yml");
        writePlan("0001-rundreise-schweiz.yml");
        PlanLoader planLoader = new PlanLoader(plansDir);

        List<String> planNames = planLoader.discoverPlanNames();

        assertThat(planNames).containsExactly("0001-rundreise-schweiz", "0002-hauptstadt-798");
    }

    @Test
    void reportsInvalidPlanFileNameDuringDiscovery() throws Exception {
        Files.writeString(plansDir.resolve("missing-prefix.yml"), "prompt: test");
        PlanLoader loader = new PlanLoader(plansDir);

        assertThatThrownBy(loader::discoverPlanNames)
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("four digit number");
    }

    private void writePlan(String relativePath) throws Exception {
        Path path = plansDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                inferenceProvider: OPENAI
                model: gpt-test
                description: |
                  offene Antwort mit Freiheitsgraden.
                  Die Reiseplanung bleibt trotzdem vergleichbar.
                  Detailauswahl ist variabel.
                analysis:
                  promptEvaluation: ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                  swissRoundTrip:
                    expectedStationCount: 5
                    language: DE
                run:
                  prompt: "Erstelle eine Rundreise"
                  iterations: 1
                  temperature: 0.0
                  topP: 1.0
                  topK: 1
                  seed: RANDOM
                  reasoning: off
                """);
    }
}
