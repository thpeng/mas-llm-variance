package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;
import java.util.Arrays;

public enum RoundTripBfsDestinationLanguageRegion {
    // Source: Swiss Federal Statistical Office, "Die Sprachgebiete der Schweiz".
    // https://www.bfs.admin.ch/bfs/rm/home.assetdetail.23705034.html
    BASEL(Destination.BASEL, 2701, "Basel", BfsLanguageRegion.D, "D", "D", 22, "2701 Basel D D 74.9 0.5"),
    BERN(Destination.BERN, 351, "Bern", BfsLanguageRegion.D, "D", "D", 10, "351 Bern D D 82.1 0.4 6.7 0.2"),
    DAVOS(Destination.DAVOS, 3851, "Davos", BfsLanguageRegion.D, "D", "D", 27, "3851 Davos D D 85.1 1.7 5.3 1.0 1.7 0.6"),
    GENEVE(Destination.GENEVE, 6621, "Geneve", BfsLanguageRegion.F, "F", "F", 41, "6621 Geneve F F 76.4 0.4"),
    GRINDELWALD(Destination.GRINDELWALD, 576, "Grindelwald", BfsLanguageRegion.D, "D", "D", 11, "576 Grindelwald D D 86.0 2.7 2.8 1.1"),
    GRUYERES(Destination.GRUYERES, 2135, "Gruyeres", BfsLanguageRegion.F, "F", "F", 18, "2135 Gruyeres F F 2.6 1.3 86.3 3.2"),
    INTERLAKEN(Destination.INTERLAKEN, 581, "Interlaken", BfsLanguageRegion.D, "D", "D", 11, "581 Interlaken D D 83.2 2.4 2.3 0.9"),
    LAUSANNE(Destination.LAUSANNE, 5586, "Lausanne", BfsLanguageRegion.F, "F", "F", 35, "5586 Lausanne F F 79.2 0.3"),
    LAUTERBRUNNEN(Destination.LAUTERBRUNNEN, 584, "Lauterbrunnen", BfsLanguageRegion.D, "D", "D", 11, "584 Lauterbrunnen D D 77.7 4.1 2.7 2.1"),
    LUCERNE(Destination.LUCERNE, 1061, "Luzern", BfsLanguageRegion.D, "D", "D", 15, "1061 Luzern D D 83.4 0.4"),
    LUGANO(Destination.LUGANO, 5192, "Lugano", BfsLanguageRegion.I, "I", "I", 33, "5192 Lugano I I 87.1 0.4"),
    MONTREUX(Destination.MONTREUX, 5886, "Montreux", BfsLanguageRegion.F, "F", "F", 37, "5886 Montreux F F 77.2 0.8"),
    SCHAFFHAUSEN(Destination.SCHAFFHAUSEN, 2939, "Schaffhausen", BfsLanguageRegion.D, "D", "D", 23, "2939 Schaffhausen D D 83.9 0.8"),
    ST_MORITZ(Destination.ST_MORITZ, 3787, "St. Moritz", BfsLanguageRegion.D, "D", "D", 27, "3787 St. Moritz D D 63.6 3.2 28.0 3.0 4.8 1.3"),
    ZERMATT(Destination.ZERMATT, 6300, "Zermatt", BfsLanguageRegion.D, "D", "D", 40, "6300 Zermatt D D 69.5 2.6 8.0 1.6"),
    ZURICH(Destination.ZURICH, 261, "Zurich", BfsLanguageRegion.D, "D", "D", 9, "261 Zurich D D 76.2 0.3");

    static final String SOURCE_URL = "https://www.bfs.admin.ch/bfs/rm/home.assetdetail.23705034.html";

    private final Destination destination;
    private final int bfsMunicipalityId;
    private final String bfsName;
    private final BfsLanguageRegion bfsLanguageRegion;
    private final String bfsPreviousLanguageRegion;
    private final String bfsResultLanguageRegion;
    private final int bfsReportPage;
    private final String bfsRowText;

    RoundTripBfsDestinationLanguageRegion(
            Destination destination,
            int bfsMunicipalityId,
            String bfsName,
            BfsLanguageRegion bfsLanguageRegion,
            String bfsPreviousLanguageRegion,
            String bfsResultLanguageRegion,
            int bfsReportPage,
            String bfsRowText
    ) {
        this.destination = destination;
        this.bfsMunicipalityId = bfsMunicipalityId;
        this.bfsName = bfsName;
        this.bfsLanguageRegion = bfsLanguageRegion;
        this.bfsPreviousLanguageRegion = bfsPreviousLanguageRegion;
        this.bfsResultLanguageRegion = bfsResultLanguageRegion;
        this.bfsReportPage = bfsReportPage;
        this.bfsRowText = bfsRowText;
    }

    public static BfsLanguageRegion region(Destination destination) {
        return Arrays.stream(values())
                .filter(entry -> entry.destination == destination)
                .findFirst()
                .map(RoundTripBfsDestinationLanguageRegion::bfsLanguageRegion)
                .orElseThrow(() -> new MetaAnalysisException("Missing BFS language region for destination: " + destination));
    }

    public Destination destination() {
        return destination;
    }

    public int bfsMunicipalityId() {
        return bfsMunicipalityId;
    }

    public String bfsName() {
        return bfsName;
    }

    public BfsLanguageRegion bfsLanguageRegion() {
        return bfsLanguageRegion;
    }

    public String bfsPreviousLanguageRegion() {
        return bfsPreviousLanguageRegion;
    }

    public String bfsResultLanguageRegion() {
        return bfsResultLanguageRegion;
    }

    public int bfsReportPage() {
        return bfsReportPage;
    }

    public String bfsRowText() {
        return bfsRowText;
    }
}
