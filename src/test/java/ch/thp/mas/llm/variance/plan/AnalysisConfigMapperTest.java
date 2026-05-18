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
        YamlAnalysisConfig.Hierarchical hierarchical = new YamlAnalysisConfig.Hierarchical();
        hierarchical.setThreshold(0.05);
        hierarchical.setLinkage(HierarchicalLinkage.COMPLETE);
        analysis.setHierarchical(hierarchical);
        plan.setAnalysis(analysis);

        var config = mapper.map(new LoadedPlan("0001-test", "0001-test.yml", plan));

        assertThat(config.embeddingProvider()).isEqualTo("local-hashing");
        assertThat(config.semanticRepresentation()).isEqualTo(SemanticRepresentation.CHUNK_AVERAGE_MIN);
        assertThat(config.clusteringAlgorithm()).isEqualTo(ClusteringAlgorithm.HIERARCHICAL);
        assertThat(config.hierarchical().threshold()).isEqualTo(0.05);
        assertThat(config.hierarchical().linkage()).isEqualTo(HierarchicalLinkage.COMPLETE);
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
