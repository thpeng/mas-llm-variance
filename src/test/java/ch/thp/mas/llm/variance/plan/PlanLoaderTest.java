package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class PlanLoaderTest {

    private final PlanLoader planLoader = new PlanLoader();

    @Test
    void loadsPlanByNameWithoutSuffix() {
        LoadedPlan loadedPlan = planLoader.load("0001-rundreise-schweiz");

        assertThat(loadedPlan.name()).isEqualTo("0001-rundreise-schweiz");
        assertThat(loadedPlan.plan().getPrompt()).contains("Rundreise");
        assertThat(((YamlPlan) loadedPlan.plan()).getRun().getSeed()).isEqualTo("RANDOM");
        assertThat(loadedPlan.plan().getSeed()).isNull();
        assertThat(((YamlPlan) loadedPlan.plan()).getDescription())
                .contains("offene Antwort")
                .contains("Freiheitsgraden.\nDie Reiseplanung")
                .contains("Detailauswahl");
        assertThat(((YamlPlan) loadedPlan.plan()).getAnalysis().getClusteringAlgorithm().name())
                .isEqualTo("HIERARCHICAL");
        assertThat(((YamlPlan) loadedPlan.plan()).getAnalysis().getHierarchical().getThreshold())
                .isEqualTo(0.08);
    }

    @Test
    void loadsPlanByNameWithSuffix() {
        LoadedPlan loadedPlan = planLoader.load("0001-rundreise-schweiz.yml");

        assertThat(loadedPlan.name()).isEqualTo("0001-rundreise-schweiz");
        assertThat(loadedPlan.filename()).isEqualTo("0001-rundreise-schweiz.yml");
    }

    @Test
    void rejectsUnknownPlanName() {
        assertThatThrownBy(() -> planLoader.load("9999-missing"))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Plan not found");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> planLoader.load("../0001-rundreise-schweiz"))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Invalid plan name");
    }

    @Test
    void discoversPlansInNaturalOrder() {
        List<String> planNames = planLoader.discoverPlanNames();

        assertThat(planNames).containsSubsequence("0001-rundreise-schweiz", "0002-hauptstadt-798");
    }

    @Test
    void reportsInvalidPlanFileNameDuringDiscovery() {
        PlanLoader loader = new PlanLoader(new StubResourceResolver(
                new NamedResource("missing-prefix.yml", "prompt: test")
        ));

        assertThatThrownBy(loader::discoverPlanNames)
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("four digit number");
    }

    private static class StubResourceResolver extends PathMatchingResourcePatternResolver {

        private final Resource[] resources;

        StubResourceResolver(Resource... resources) {
            this.resources = resources;
        }

        @Override
        public Resource[] getResources(String locationPattern) throws IOException {
            return locationPattern.endsWith(".yml") ? resources : new Resource[0];
        }
    }

    private static class NamedResource extends ByteArrayResource {

        private final String filename;

        NamedResource(String filename, String content) {
            super(content.getBytes());
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
