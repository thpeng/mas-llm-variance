package ch.thp.mas.llm.variance.metanalysis;

public enum BfsLanguageRegion {
    D("German"),
    F("French"),
    I("Italian"),
    R("Romansh");

    private final String label;

    BfsLanguageRegion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
