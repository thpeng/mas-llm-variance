package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.analyze.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;

public class YamlAnalysisConfig {

    private ClusteringAlgorithm clusteringAlgorithm;
    private Route route;
    private FactualTravelInfo factualTravelInfo;
    private LiteralFormatTravelerGuidance literalFormatTravelerGuidance;
    private CreativeMarketingText creativeMarketingText;
    private Bleu bleu;
    private Rouge rouge;

    public ClusteringAlgorithm getClusteringAlgorithm() {
        return clusteringAlgorithm;
    }

    public void setClusteringAlgorithm(ClusteringAlgorithm clusteringAlgorithm) {
        this.clusteringAlgorithm = clusteringAlgorithm;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public FactualTravelInfo getFactualTravelInfo() {
        return factualTravelInfo;
    }

    public void setFactualTravelInfo(FactualTravelInfo factualTravelInfo) {
        this.factualTravelInfo = factualTravelInfo;
    }

    public LiteralFormatTravelerGuidance getLiteralFormatTravelerGuidance() {
        return literalFormatTravelerGuidance;
    }

    public void setLiteralFormatTravelerGuidance(LiteralFormatTravelerGuidance literalFormatTravelerGuidance) {
        this.literalFormatTravelerGuidance = literalFormatTravelerGuidance;
    }

    public CreativeMarketingText getCreativeMarketingText() {
        return creativeMarketingText;
    }

    public void setCreativeMarketingText(CreativeMarketingText creativeMarketingText) {
        this.creativeMarketingText = creativeMarketingText;
    }

    public Bleu getBleu() {
        return bleu;
    }

    public void setBleu(Bleu bleu) {
        this.bleu = bleu;
    }

    public Rouge getRouge() {
        return rouge;
    }

    public void setRouge(Rouge rouge) {
        this.rouge = rouge;
    }

    public static class Route {
        private Integer expectedStationCount;

        public Integer getExpectedStationCount() {
            return expectedStationCount;
        }

        public void setExpectedStationCount(Integer expectedStationCount) {
            this.expectedStationCount = expectedStationCount;
        }
    }

    public static class FactualTravelInfo {
        private String departureFromBern;
        private String arrivalAtZurich;
        private Integer changes;

        public String getDepartureFromBern() {
            return departureFromBern;
        }

        public void setDepartureFromBern(String departureFromBern) {
            this.departureFromBern = departureFromBern;
        }

        public String getArrivalAtZurich() {
            return arrivalAtZurich;
        }

        public void setArrivalAtZurich(String arrivalAtZurich) {
            this.arrivalAtZurich = arrivalAtZurich;
        }

        public Integer getChanges() {
            return changes;
        }

        public void setChanges(Integer changes) {
            this.changes = changes;
        }
    }

    public static class LiteralFormatTravelerGuidance {
        private String reference;

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }
    }

    public static class CreativeMarketingText {
        private Integer expectedSentenceCount;
        private String requiredTerm;

        public Integer getExpectedSentenceCount() {
            return expectedSentenceCount;
        }

        public void setExpectedSentenceCount(Integer expectedSentenceCount) {
            this.expectedSentenceCount = expectedSentenceCount;
        }

        public String getRequiredTerm() {
            return requiredTerm;
        }

        public void setRequiredTerm(String requiredTerm) {
            this.requiredTerm = requiredTerm;
        }
    }

    public static class Bleu {
        private Integer maxN;
        private Double smoothingEpsilon;

        public Integer getMaxN() {
            return maxN;
        }

        public void setMaxN(Integer maxN) {
            this.maxN = maxN;
        }

        public Double getSmoothingEpsilon() {
            return smoothingEpsilon;
        }

        public void setSmoothingEpsilon(Double smoothingEpsilon) {
            this.smoothingEpsilon = smoothingEpsilon;
        }
    }

    public static class Rouge {
        private RougeConfig.Variant variant;
        private RougeConfig.Aggregation aggregation;

        public RougeConfig.Variant getVariant() {
            return variant;
        }

        public void setVariant(RougeConfig.Variant variant) {
            this.variant = variant;
        }

        public RougeConfig.Aggregation getAggregation() {
            return aggregation;
        }

        public void setAggregation(RougeConfig.Aggregation aggregation) {
            this.aggregation = aggregation;
        }
    }

}
