package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SwissRoundTripStationExtractorTest {

    private final SwissRoundTripStationExtractor extractor = new SwissRoundTripStationExtractor();

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

    @Test
    void extractsPlainNumberedStationsUntilEmDash() {
        String response = """
                1. Zurich — a convenient international gateway with easy onward travel.
                2. Lucerne — a scenic lakeside city with classic Swiss charm.
                3. Interlaken — ideal for alpine views and mountain excursions.
                4. Zermatt — famous for Matterhorn scenery and car-free mountain atmosphere.
                5. Geneva — a polished lakeside finale with excellent transport links.
                """;

        assertThat(extractor.extract(response))
                .containsExactly("Zurich", "Lucerne", "Interlaken", "Zermatt", "Geneva");
    }

    @Test
    void extractsBoldNumberedLineStations() {
        String response = """
                # Circuit en Suisse - 5 etapes

                **1. Geneve**
                Point de depart ideal.

                **2. Lausanne**
                Ville dynamique sur les rives du lac Leman.

                **3. Interlaken**
                Situe entre deux lacs.
                """;

        assertThat(extractor.extract(response)).containsExactly("Geneve", "Lausanne", "Interlaken");
    }
}
