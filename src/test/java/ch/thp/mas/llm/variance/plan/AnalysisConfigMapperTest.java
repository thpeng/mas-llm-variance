package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.AnalysisException;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import org.junit.jupiter.api.Test;

class AnalysisConfigMapperTest {

    private final AnalysisConfigMapper mapper = new AnalysisConfigMapper();

    @Test
    void mapsYamlAnalysisBlockOntoAnalysisConfig() {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setEmbeddingProvider("local-hashing");
        analysis.setSemanticRepresentation(SemanticRepresentation.CHUNK_AVERAGE_MIN);
        analysis.setClusteringAlgorithm(ClusteringAlgorithm.HIERARCHICAL);
        analysis.setScanIncrement(0.01);
        YamlAnalysisConfig.Hierarchical hierarchical = new YamlAnalysisConfig.Hierarchical();
        YamlAnalysisConfig.Range threshold = new YamlAnalysisConfig.Range();
        threshold.setFrom(0.05);
        threshold.setTo(0.07);
        hierarchical.setThreshold(threshold);
        hierarchical.setLinkage(HierarchicalLinkage.COMPLETE);
        analysis.setHierarchical(hierarchical);
        YamlAnalysisConfig.FactualTravelInfo factualTravelInfo = new YamlAnalysisConfig.FactualTravelInfo();
        factualTravelInfo.setDepartureFromBern("08:02");
        factualTravelInfo.setArrivalAtZurich("09:15");
        factualTravelInfo.setChanges(0);
        analysis.setFactualTravelInfo(factualTravelInfo);
        plan.setAnalysis(analysis);

        var config = mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan));

        assertThat(config.embeddingProvider()).isEqualTo("local-hashing");
        assertThat(config.semanticRepresentation()).isEqualTo(SemanticRepresentation.CHUNK_AVERAGE_MIN);
        assertThat(config.clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.HIERARCHICAL);
        assertThat(config.scanIncrement()).isEqualTo(0.01);
        assertThat(config.hierarchical().threshold().from()).isEqualTo(0.05);
        assertThat(config.hierarchical().threshold().to()).isEqualTo(0.07);
        assertThat(config.hierarchical().linkage()).isEqualTo(HierarchicalLinkage.COMPLETE);
        assertThat(config.factualTravelInfo().departureFromBern()).isEqualTo("08:02");
        assertThat(config.factualTravelInfo().arrivalAtZurich()).isEqualTo("09:15");
        assertThat(config.factualTravelInfo().changes()).isZero();
    }

    @Test
    void rejectsScalarClusteringParameters() {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setClusteringAlgorithm(ClusteringAlgorithm.DBSCAN);
        YamlAnalysisConfig.Dbscan dbscan = new YamlAnalysisConfig.Dbscan();
        dbscan.setEpsilon(0.06);
        analysis.setDbscan(dbscan);
        plan.setAnalysis(analysis);

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("analysis.dbscan.epsilon")
                .hasMessageContaining("from/to");
    }

    @Test
    void rejectsIncompleteRanges() {
        YamlPlan plan = new YamlPlan();
        YamlAnalysisConfig analysis = new YamlAnalysisConfig();
        analysis.setClusteringAlgorithm(ClusteringAlgorithm.HIERARCHICAL);
        YamlAnalysisConfig.Hierarchical hierarchical = new YamlAnalysisConfig.Hierarchical();
        YamlAnalysisConfig.Range threshold = new YamlAnalysisConfig.Range();
        threshold.setFrom(0.05);
        hierarchical.setThreshold(threshold);
        analysis.setHierarchical(hierarchical);
        plan.setAnalysis(analysis);

        assertThatThrownBy(() -> mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("analysis.hierarchical.threshold.to");
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
