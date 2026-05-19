package ch.thp.mas.llm.variance.analyze.route;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RouteStationExtractorTest {

    private final RouteStationExtractor extractor = new RouteStationExtractor();

    @Test
    void extractsMarkdownBoldNumberedStations() {
        String response = """
                1. **Genève** : Point de départ.
                2. **Zermatt** : Étape incontournable.
                3. **Interlaken** - Située entre deux lacs.
                """;

        assertThat(extractor.extract(response)).containsExactly("Genève", "Zermatt", "Interlaken");
    }

    @Test
    void extractsPlainNumberedStationsUntilColonOrHyphen() {
        String response = """
                1. Genève: Point de départ.
                2. Zermatt: Étape incontournable.
                3. Interlaken - Située entre deux lacs.
                """;

        assertThat(extractor.extract(response)).containsExactly("Genève", "Zermatt", "Interlaken");
    }

    @Test
    void extractsMarkdownHeadingStationsUntilParenthesis() {
        String response = """
                #### 1. Luzern (Das Tor zur Zentralschweiz)
                #### 2. Interlaken & Lauterbrunnen (Das Herz der Alpen)
                #### 3. Zermatt (Das Bergsteiger-Dorf)
                """;

        assertThat(extractor.extract(response)).containsExactly("Luzern", "Interlaken & Lauterbrunnen", "Zermatt");
    }
}
