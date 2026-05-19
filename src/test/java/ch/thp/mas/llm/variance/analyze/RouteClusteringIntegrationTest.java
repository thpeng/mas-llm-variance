package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.Destination;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalyzer;
import ch.thp.mas.llm.variance.analyze.route.RouteConfig;
import ch.thp.mas.llm.variance.analyze.route.RouteExtractionStatus;
import ch.thp.mas.llm.variance.analyze.route.RouteStationExtractor;
import ch.thp.mas.llm.variance.analyze.semantic.AnswerChunker;
import ch.thp.mas.llm.variance.analyze.semantic.ChunkAverageMinDistance;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.CosineDistance;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalClusterer;
import ch.thp.mas.llm.variance.analyze.semantic.MedoidSelector;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.LmStudioLoadConfigLog;
import ch.thp.mas.llm.variance.run.ModelInstanceLog;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteClusteringIntegrationTest {

    private static final String FRENCH_ROUTE_RESPONSE = """
            1. **Zurich** : Point de départ stratégique offrant un excellent accès aux transports publics et une introduction à la culture urbaine suisse.
            2. **Lucerne** : Située sur le lac, elle permet d'admirer les célèbres ponts médiévaux et de commencer l'ascension vers les Alpes.
            3. **Interlaken** : Cœur du tourisme alpin, idéal pour explorer la région des Jungfrau et profiter des paysages de montagne spectaculaires.
            4. **Zermatt** : Destination incontournable pour voir le Matterhorn et découvrir une ville sans voiture au sommet des Alpes.
            5. **Geneve** : Fin de voyage dans une ville cosmopolite connue pour ses horloges, son lac et sa proximité avec la France.
            """;

    private static final String GOOGLE_FLASH_ST_MORITZ_ROUTE = """
            Hier ist ein Vorschlag fuer eine abwechslungsreiche Rundreise durch die Schweiz.

            ### Die Route: "Best of Switzerland"

            #### 1. Luzern (Das Tor zur Zentralschweiz)
            Luzern liegt direkt am Vierwaldstaettersee.

            #### 2. Interlaken & Lauterbrunnen (Das Herz der Alpen)
            Diese Region im Berner Oberland bietet das klassische Alpenpanorama.

            #### 3. Zermatt (Das Bergsteiger-Dorf)
            Zermatt ist autofrei und beheimatet das Matterhorn.

            #### 4. Lugano (Das Tessiner Dolce Vita)
            Hier trifft Schweizer Effizienz auf italienisches Lebensgefuehl.

            #### 5. St. Moritz oder Engadin (Luxus & Natur pur)
            Das Oberengadin ist ein Hochtal mit tiefblauen Seen.
            """;

    private static final String GOOGLE_FLASH_ZURICH_ROUTE = """
            Hier ist ein Vorschlag fuer eine abwechslungsreiche Rundreise durch die Schweiz.

            ### Die Route: "Best of Switzerland"

            #### 1. Luzern (Das Tor zur Zentralschweiz)
            Luzern ist der perfekte Startpunkt.

            #### 2. Interlaken & Lauterbrunnen (Das Herz der Alpen)
            Interlaken liegt zwischen dem Thuner- und Brienzersee.

            #### 3. Zermatt (Das Matterhorn-Erlebnis)
            Zermatt bietet den besten Blick auf den Berg der Berge.

            #### 4. Lugano (Mediterranes Flair im Tessin)
            Lugano bietet Palmen, milde Temperaturen und Dolce Vita.

            #### 5. Zuerich (Urbaner Lifestyle & Geschichte)
            Zum Abschluss geht es zurueck in den Norden.
            """;

    private static final String GOOGLE_FLASH_ZURICH_OVERVIEW_ROUTE = """
            Diese Rundreise fuehrt dich zu den absoluten Highlights der Schweiz.

            ### Die Route im Ueberblick:
            **Zuerich - Luzern - Interlaken - Zermatt - Lugano**

            ### 1. Luzern (Das Herz der Schweiz)
            Luzern ist der perfekte Startpunkt.

            ### 2. Interlaken & Jungfrau Region (Das Berner Oberland)
            Interlaken ist das Tor zu den spektakulaersten Gipfeln der Alpen.

            ### 3. Zermatt (Das Matterhorn)
            Zermatt ist autofrei und bietet den beruehmtesten Bergblick.

            ### 4. Lugano (Das Tessin - Mediterranes Flair)
            Nach den hohen Bergen geht es in den Sueden.

            ### 5. Zuerich (Die urbane Metropole)
            Zum Abschluss geht es zurueck in den Norden.
            """;

    private static final String GOOGLE_FLASH_AMBIGUOUS_GRAUBUENDEN_ROUTE = """
            Diese Rundreise fuehrt dich zu den absoluten Highlights der Schweiz.

            #### 1. Luzern (Zentralschweiz) - Das Tor zu den Alpen
            Luzern ist der perfekte Startpunkt.

            #### 2. Interlaken & Lauterbrunnen (Berner Oberland) - Das Herz der Alpen
            Von Luzern geht es ueber den Bruenigpass ins Berner Oberland.

            #### 3. Zermatt (Wallis) - Der Berg der Berge
            Weiter suedlich erreichst du das autofreie Dorf Zermatt.

            #### 4. Lugano (Tessin) - Mediterranes Flair
            Durch den Gotthard-Tunnel gelangst du in den italienischsprachigen Teil der Schweiz.

            #### 5. St. Moritz oder Chur (Graubuenden) - Alpine Eleganz & Gletscher
            Mit dem Bernina Express geht es in den Kanton Graubuenden.
            """;

    @Test
    void analyzesFrenchQwenRundreiseRunWithRouteClustering() {
        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("0003-qwen35-rundreise-french-run.json", runLog(50, FRENCH_ROUTE_RESPONSE)),
                routeConfig()
        );

        assertThat(result.scans()).isEmpty();
        assertThat(result.route()).isNotNull();
        assertThat(result.route().responseCount()).isEqualTo(50);
        assertThat(result.route().successfulExtractionCount()).isEqualTo(50);
        assertThat(result.route().partialExtractionCount()).isZero();
        assertThat(result.route().failedExtractionCount()).isZero();
        assertThat(result.route().unknownNameCount()).isZero();
        assertThat(result.route().uniqueRouteCount()).isEqualTo(1);
        assertThat(result.route().topRouteShare()).isEqualTo(1.0);
        assertThat(result.route().clusters()).hasSize(1);
        assertThat(result.route().clusters().getFirst().routeKey())
                .isEqualTo("ZURICH|LUCERNE|INTERLAKEN|ZERMATT|GENEVE");
        assertThat(result.route().clusters().getFirst().size()).isEqualTo(50);
        assertThat(result.route().outliers()).isEmpty();
        assertThat(result.route().extractions())
                .allSatisfy(extraction -> assertThat(extraction.extractionStatus())
                        .isEqualTo(RouteExtractionStatus.SUCCESS));

        assertThat(result.route().stationFrequencies())
                .filteredOn(frequency -> List.of(
                        Destination.ZURICH,
                        Destination.LUCERNE,
                        Destination.INTERLAKEN,
                        Destination.ZERMATT,
                        Destination.GENEVE
                ).contains(frequency.destination()))
                .allSatisfy(frequency -> {
                    assertThat(frequency.count()).isEqualTo(50);
                    assertThat(frequency.shareOfSuccessfulExtractions()).isEqualTo(1.0);
                });
        assertThat(result.route().positionDistributions().get(0).frequencies().getFirst().destination())
                .isEqualTo(Destination.ZURICH);
        assertThat(result.route().positionDistributions().get(4).frequencies().getFirst().destination())
                .isEqualTo(Destination.GENEVE);
        assertThat(result.route().pairwiseJaccard().count()).isEqualTo(1225);
        assertThat(result.route().pairwiseJaccard().min()).isEqualTo(1.0);
        assertThat(result.route().pairwiseJaccard().p10()).isEqualTo(1.0);
        assertThat(result.route().pairwiseJaccard().median()).isEqualTo(1.0);
        assertThat(result.route().pairwiseJaccard().p90()).isEqualTo(1.0);
        assertThat(result.route().pairwiseJaccard().max()).isEqualTo(1.0);
        assertThat(result.route().pairwiseJaccard().mean()).isEqualTo(1.0);
        assertThat(result.route().syntactic().clusters()).hasSize(1);
        assertThat(result.literal().responseCount()).isEqualTo(50);
    }

    @Test
    void treatsPartialAndUnknownRoutesAsOutliers() {
        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("route-outliers.json", runLog(
                        FRENCH_ROUTE_RESPONSE,
                        """
                                1. Zurich: Start.
                                2. Lucerne: Lake.
                                3. Interlaken: Alps.
                                4. Zermatt: Matterhorn.
                                """,
                        """
                                1. Zurich: Start.
                                2. Atlantis: Mystery.
                                3. Interlaken: Alps.
                                4. Zermatt: Matterhorn.
                                5. Geneve: City.
                                """
                )),
                routeConfig()
        );

        assertThat(result.route().successfulExtractionCount()).isEqualTo(1);
        assertThat(result.route().partialExtractionCount()).isEqualTo(2);
        assertThat(result.route().failedExtractionCount()).isZero();
        assertThat(result.route().unknownNameCount()).isEqualTo(1);
        assertThat(result.route().outliers()).containsExactly(2, 3);
        assertThat(result.route().extractions().get(2).unknownNames()).containsExactly("Atlantis");
    }

    @Test
    void analyzesGoogleFlashRundreiseRunWithRouteClustering() {
        List<String> responses = List.of(
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE.replace("St. Moritz oder Engadin", "St. Moritz & Engadin"),
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE.replace("St. Moritz oder Engadin", "St. Moritz & Engadin"),
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_ST_MORITZ_ROUTE.replace("St. Moritz oder Engadin", "St. Moritz & Engadin"),
                GOOGLE_FLASH_ZURICH_OVERVIEW_ROUTE,
                GOOGLE_FLASH_ZURICH_ROUTE,
                GOOGLE_FLASH_AMBIGUOUS_GRAUBUENDEN_ROUTE
        );

        AnalysisResult result = analyzer().analyze(
                new NamedRunLog("0001-google-flash-rundreise-run.json", googleFlashRunLog(responses)),
                routeConfig()
        );

        assertThat(result.scans()).isEmpty();
        assertThat(result.route().responseCount()).isEqualTo(20);
        assertThat(result.route().successfulExtractionCount()).isZero();
        assertThat(result.route().partialExtractionCount()).isEqualTo(20);
        assertThat(result.route().failedExtractionCount()).isZero();
        assertThat(result.route().unknownNameCount()).isEqualTo(31);
        assertThat(result.route().uniqueRouteCount()).isZero();
        assertThat(result.route().topRouteShare()).isNull();
        assertThat(result.route().outliers()).containsExactly(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20
        );
        assertThat(result.route().extractions().getFirst().unknownNames())
                .containsExactly("Interlaken & Lauterbrunnen", "St. Moritz oder Engadin");
        assertThat(result.route().extractions().get(4).unknownNames())
                .containsExactly("Interlaken & Lauterbrunnen");
        assertThat(result.route().extractions().get(17).unknownNames())
                .containsExactly("Interlaken & Jungfrau Region");
        assertThat(result.route().extractions().get(19).unknownNames())
                .containsExactly("Interlaken & Lauterbrunnen", "St. Moritz oder Chur");

        assertThat(result.route().clusters()).isEmpty();
        assertThat(result.route().stationFrequencies()).isEmpty();
        assertThat(result.route().positionDistributions())
                .allSatisfy(distribution -> assertThat(distribution.frequencies()).isEmpty());
        assertThat(result.route().pairwiseJaccard().count()).isZero();
        assertThat(result.route().pairwiseJaccard().min()).isNull();
        assertThat(result.route().pairwiseJaccard().p10()).isNull();
        assertThat(result.route().pairwiseJaccard().median()).isNull();
        assertThat(result.route().pairwiseJaccard().p90()).isNull();
        assertThat(result.route().pairwiseJaccard().max()).isNull();
        assertThat(result.route().pairwiseJaccard().mean()).isNull();
        assertThat(result.route().syntactic().clusters()).isEmpty();
        assertThat(result.literal().responseCount()).isEqualTo(20);
    }

    private static Analyzer analyzer() {
        TextTokenizer tokenizer = new TextTokenizer();
        CosineDistance cosineDistance = new CosineDistance();
        return new Analyzer(
                (texts, config) -> {
                    throw new AssertionError("Route clustering must not call the embedding service.");
                },
                cosineDistance,
                new ChunkAverageMinDistance(cosineDistance),
                new MedoidSelector(),
                new DbscanClusterer(),
                new HierarchicalClusterer(),
                new RouteAnalyzer(new RouteStationExtractor()),
                new AnswerChunker(tokenizer),
                new RougeLMetric(tokenizer),
                new BleuMetric(tokenizer),
                new LiteralAnalyzer(),
                new SummaryStatistics(),
                new FixedClock(),
                AnalysisConfig::defaults
        );
    }

    private static AnalysisConfig routeConfig() {
        AnalysisConfig defaults = AnalysisConfig.defaults();
        return new AnalysisConfig(
                defaults.embeddingProvider(),
                defaults.embeddingBaseUrl(),
                defaults.embeddingModel(),
                defaults.embeddingPrefix(),
                defaults.maxEmbeddingTokens(),
                defaults.semanticDistanceMethod(),
                defaults.semanticRepresentation(),
                defaults.chunk(),
                defaults.distance(),
                ClusteringAlgorithm.ROUTE,
                defaults.scanIncrement(),
                defaults.dbscan(),
                defaults.hierarchical(),
                new RouteConfig(5),
                defaults.bleu(),
                defaults.rouge(),
                defaults.percentile()
        );
    }

    private static RunLog runLog(int repetitions, String response) {
        return runLog(java.util.stream.IntStream.range(0, repetitions)
                .mapToObj(ignored -> response)
                .toArray(String[]::new));
    }

    private static RunLog runLog(String... responses) {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-18T21:11:37.885186+02:00");
        List<RunLogEntry> entries = java.util.stream.IntStream.range(0, responses.length)
                .mapToObj(index -> new RunLogEntry(index + 1, startedAt, startedAt, responses[index], null))
                .toList();
        return new RunLog(
                "0003-qwen35-rundreise-french",
                startedAt,
                OffsetDateTime.parse("2026-05-18T21:13:00.733025300+02:00"),
                InferenceProvider.LMSTUDIO,
                "qwen/qwen3.5-9b",
                null,
                new ModelInstanceLog(
                        "qwen/qwen3.5-9b",
                        false,
                        new LmStudioLoadConfigLog(null, null, null, null, null),
                        (JsonNode) null
                ),
                responses.length,
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "Crée une courte planification pour un voyage en circuit à travers la Suisse avec 5 étapes. Nomme uniquement les 5 étapes dans un ordre logique et donne une brève raison pour chaque choix.",
                entries
        );
    }

    private static RunLog googleFlashRunLog(List<String> responses) {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-18T13:34:39.722927500+02:00");
        List<RunLogEntry> entries = java.util.stream.IntStream.range(0, responses.size())
                .mapToObj(index -> new RunLogEntry(index + 1, startedAt, startedAt, responses.get(index), null))
                .toList();
        return new RunLog(
                "0001-google-flash-rundreise",
                startedAt,
                OffsetDateTime.parse("2026-05-18T13:36:19.302666300+02:00"),
                InferenceProvider.GOOGLE,
                "gemini-3-flash-preview",
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an",
                entries
        );
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-19T10:00:00+02:00");
        }
    }
}
