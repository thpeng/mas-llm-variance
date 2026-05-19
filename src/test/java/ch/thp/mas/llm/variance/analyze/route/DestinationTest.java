package ch.thp.mas.llm.variance.analyze.route;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DestinationTest {

    @Test
    void normalizesKnownDestinationVariants() {
        assertThat(Destination.fromRawName("Genève")).contains(Destination.GENEVE);
        assertThat(Destination.fromRawName("Geneve")).contains(Destination.GENEVE);
        assertThat(Destination.fromRawName("Genf")).contains(Destination.GENEVE);
        assertThat(Destination.fromRawName("Zürich")).contains(Destination.ZURICH);
        assertThat(Destination.fromRawName("Zuerich")).contains(Destination.ZURICH);
        assertThat(Destination.fromRawName("Lucerne")).contains(Destination.LUCERNE);
        assertThat(Destination.fromRawName("Luzern")).contains(Destination.LUCERNE);
        assertThat(Destination.fromRawName("St. Moritz")).contains(Destination.ST_MORITZ);
        assertThat(Destination.fromRawName("Saint Moritz")).contains(Destination.ST_MORITZ);
        assertThat(Destination.fromRawName("Sankt Moritz")).contains(Destination.ST_MORITZ);
        assertThat(Destination.fromRawName("Chur")).contains(Destination.CHUR);
        assertThat(Destination.fromRawName("Coire")).contains(Destination.CHUR);
        assertThat(Destination.fromRawName("Coira")).contains(Destination.CHUR);
        assertThat(Destination.fromRawName("Cuira")).contains(Destination.CHUR);
        assertThat(Destination.fromRawName("Lausanne")).contains(Destination.LAUSANNE);
        assertThat(Destination.fromRawName("Lausana")).contains(Destination.LAUSANNE);
        assertThat(Destination.fromRawName("Losanna")).contains(Destination.LAUSANNE);
        assertThat(Destination.fromRawName("Locarno")).contains(Destination.LOCARNO);
        assertThat(Destination.fromRawName("Grindelwald")).contains(Destination.GRINDELWALD);
        assertThat(Destination.fromRawName("Jungfraujoch")).contains(Destination.JUNGFRAUJOCH);
        assertThat(Destination.fromRawName("Lauterbrunnen")).contains(Destination.LAUTERBRUNNEN);
        assertThat(Destination.fromRawName("Muerren")).contains(Destination.MUERREN);
        assertThat(Destination.fromRawName("Mürren")).contains(Destination.MUERREN);
        assertThat(Destination.fromRawName("Pilatus")).contains(Destination.PILATUS);
        assertThat(Destination.fromRawName("Rigi")).contains(Destination.RIGI);
        assertThat(Destination.fromRawName("Saas Fee")).contains(Destination.SAAS_FEE);
        assertThat(Destination.fromRawName("Saas-Fee")).contains(Destination.SAAS_FEE);
        assertThat(Destination.fromRawName("Schaffhouse")).contains(Destination.SCHAFFHAUSEN);
        assertThat(Destination.fromRawName("Sciaffusa")).contains(Destination.SCHAFFHAUSEN);
        assertThat(Destination.fromRawName("Saint Gallen")).contains(Destination.ST_GALLEN);
        assertThat(Destination.fromRawName("San Gallo")).contains(Destination.ST_GALLEN);
        assertThat(Destination.fromRawName("Thoune")).contains(Destination.THUN);
        assertThat(Destination.fromRawName("Greyerz")).contains(Destination.GRUYERES);
        assertThat(Destination.fromRawName("Friburgo")).contains(Destination.FRIBOURG);
        assertThat(Destination.fromRawName("Neuenburg")).contains(Destination.NEUCHATEL);
        assertThat(Destination.fromRawName("Sitten")).contains(Destination.SION);
        assertThat(Destination.fromRawName("Zoug")).contains(Destination.ZUG);
    }

    @Test
    void doesNotNormalizeCompoundOrRegionalDestinationNames() {
        assertThat(Destination.fromRawName("Interlaken & Lauterbrunnen")).isEmpty();
        assertThat(Destination.fromRawName("Interlaken und Lauterbrunnen")).isEmpty();
        assertThat(Destination.fromRawName("Interlaken & Jungfrau Region")).isEmpty();
        assertThat(Destination.fromRawName("St. Moritz & Engadin")).isEmpty();
        assertThat(Destination.fromRawName("St. Moritz oder Engadin")).isEmpty();
        assertThat(Destination.fromRawName("St. Moritz oder Chur")).isEmpty();
        assertThat(Destination.fromRawName("Engadin")).isEmpty();
    }
}
