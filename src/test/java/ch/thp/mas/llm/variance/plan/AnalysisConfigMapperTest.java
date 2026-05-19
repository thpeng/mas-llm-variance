package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.ClusteringAlgorithm;
import org.junit.jupiter.api.Test;

class AnalysisConfigMapperTest {

    private final AnalysisConfigMapper mapper = new AnalysisConfigMapper();

    @Test
    void mapsYamlAnalysisBlockOntoAnalysisConfig() {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setClusteringAlgorithm(ClusteringAlgorithm.ROUTE);
        YamlAnalysisConfig.Route route = new YamlAnalysisConfig.Route();
        route.setExpectedStationCount(5);
        analysis.setRoute(route);
        YamlAnalysisConfig.FactualTravelInfo factualTravelInfo = new YamlAnalysisConfig.FactualTravelInfo();
        factualTravelInfo.setDepartureFromBern("08:02");
        factualTravelInfo.setArrivalAtZurich("09:15");
        factualTravelInfo.setChanges(0);
        analysis.setFactualTravelInfo(factualTravelInfo);
        YamlAnalysisConfig.LiteralFormatTravelerGuidance literalFormat = new YamlAnalysisConfig.LiteralFormatTravelerGuidance();
        literalFormat.setReference("Reisende ab Bern bis Zuerich benuetzen ab Bern bis Bern Wankdorf die Linie S3.");
        analysis.setLiteralFormatTravelerGuidance(literalFormat);
        YamlAnalysisConfig.CreativeMarketingText creativeMarketingText = new YamlAnalysisConfig.CreativeMarketingText();
        creativeMarketingText.setExpectedSentenceCount(3);
        creativeMarketingText.setRequiredTerm("Luzern");
        analysis.setCreativeMarketingText(creativeMarketingText);
        plan.setAnalysis(analysis);

        var config = mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan));

        assertThat(config.clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.ROUTE);
        assertThat(config.route().expectedStationCount()).isEqualTo(5);
        assertThat(config.factualTravelInfo().departureFromBern()).isEqualTo("08:02");
        assertThat(config.factualTravelInfo().arrivalAtZurich()).isEqualTo("09:15");
        assertThat(config.factualTravelInfo().changes()).isZero();
        assertThat(config.literalFormatTravelerGuidance().reference())
                .isEqualTo("Reisende ab Bern bis Zuerich benuetzen ab Bern bis Bern Wankdorf die Linie S3.");
        assertThat(config.creativeMarketingText().expectedSentenceCount()).isEqualTo(3);
        assertThat(config.creativeMarketingText().requiredTerm()).isEqualTo("Luzern");
    }

    @Test
    void rejectsMissingAnalysisBlock() {
        YamlPlan plan = new YamlPlan();

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("Missing analysis block");
    }

    @Test
    void rejectsMissingClusteringAlgorithm() {
        YamlPlan plan = new YamlPlan();
        plan.setAnalysis(new YamlAnalysisConfig());

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("analysis.clusteringAlgorithm");
    }
}
