package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import org.junit.jupiter.api.Test;

class AnalysisConfigMapperTest {

    private final AnalysisConfigMapper mapper = new AnalysisConfigMapper();

    @Test
    void mapsYamlAnalysisBlockOntoAnalysisConfig() {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setPromptEvaluation(PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP);
        YamlAnalysisConfig.SwissRoundTrip swissRoundTrip = new YamlAnalysisConfig.SwissRoundTrip();
        swissRoundTrip.setExpectedStationCount(5);
        analysis.setSwissRoundTrip(swissRoundTrip);
        YamlAnalysisConfig.BernZurichConnection bernZurichConnection = new YamlAnalysisConfig.BernZurichConnection();
        bernZurichConnection.setDepartureFromBern("08:02");
        bernZurichConnection.setArrivalAtZurich("09:15");
        bernZurichConnection.setChanges(0);
        analysis.setBernZurichConnection(bernZurichConnection);
        YamlAnalysisConfig.TravelerGuidanceFormat literalFormat = new YamlAnalysisConfig.TravelerGuidanceFormat();
        literalFormat.setReference("Reisende ab Bern bis Zuerich benuetzen ab Bern bis Bern Wankdorf die Linie S3.");
        analysis.setTravelerGuidanceFormat(literalFormat);
        YamlAnalysisConfig.LucerneMarketingText lucerneMarketingText = new YamlAnalysisConfig.LucerneMarketingText();
        lucerneMarketingText.setExpectedSentenceCount(3);
        lucerneMarketingText.setRequiredTerm("Luzern");
        analysis.setLucerneMarketingText(lucerneMarketingText);
        plan.setAnalysis(analysis);

        var config = mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan));

        assertThat(config.promptEvaluation()).isEqualTo(PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP);
        assertThat(config.swissRoundTrip().expectedStationCount()).isEqualTo(5);
        assertThat(config.bernZurichConnection().departureFromBern()).isEqualTo("08:02");
        assertThat(config.bernZurichConnection().arrivalAtZurich()).isEqualTo("09:15");
        assertThat(config.bernZurichConnection().changes()).isZero();
        assertThat(config.travelerGuidanceFormat().reference())
                .isEqualTo("Reisende ab Bern bis Zuerich benuetzen ab Bern bis Bern Wankdorf die Linie S3.");
        assertThat(config.lucerneMarketingText().expectedSentenceCount()).isEqualTo(3);
        assertThat(config.lucerneMarketingText().requiredTerm()).isEqualTo("Luzern");
    }

    @Test
    void rejectsMissingAnalysisBlock() {
        YamlPlan plan = new YamlPlan();

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("Missing analysis block");
    }

    @Test
    void rejectsMissingPromptEvaluation() {
        YamlPlan plan = new YamlPlan();
        plan.setAnalysis(new YamlAnalysisConfig());

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("analysis.promptEvaluation");
    }
}
