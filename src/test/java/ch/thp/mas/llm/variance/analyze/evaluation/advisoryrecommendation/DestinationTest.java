package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

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
        assertThat(Destination.fromRawName("Lauterbrunnen")).contains(Destination.LAUTERBRUNNEN);
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
        assertThat(Destination.fromRawName("Basilea")).contains(Destination.BASEL);
        assertThat(Destination.fromRawName("St.Gallen")).contains(Destination.ST_GALLEN);
        assertThat(Destination.fromRawName("St Gallen")).contains(Destination.ST_GALLEN);
        assertThat(Destination.fromRawName("St-Moritz")).contains(Destination.ST_MORITZ);
        assertThat(Destination.fromRawName("Saint-Moritz")).contains(Destination.ST_MORITZ);
        assertThat(Destination.fromRawName("Rapperswil")).contains(Destination.RAPPERSWIL_JONA);
        assertThat(Destination.fromRawName("Baden")).contains(Destination.BADEN);
        assertThat(Destination.fromRawName("Baden bei Zürich")).contains(Destination.BADEN);
        assertThat(Destination.fromRawName("Saanen")).contains(Destination.SAANEN);
        assertThat(Destination.fromRawName("Biel/Bienne")).contains(Destination.BIEL_BIENNE);
        assertThat(Destination.fromRawName("Engelberg")).contains(Destination.ENGELBERG);
        assertThat(Destination.fromRawName("Brig")).contains(Destination.BRIG_GLIS);
        assertThat(Destination.fromRawName("Bulle")).contains(Destination.BULLE);
        assertThat(Destination.fromRawName("Burgdorf")).contains(Destination.BURGDORF);
        assertThat(Destination.fromRawName("Yverdon-les-Bains")).contains(Destination.YVERDON_LES_BAINS);
        assertThat(Destination.fromRawName("Küssnacht (SZ)")).contains(Destination.KUESSNACHT_SZ);
        assertThat(Destination.fromRawName("Brienz (BE)")).contains(Destination.BRIENZ);
        assertThat(Destination.fromRawName("Glarus Süd")).contains(Destination.GLARUS_SUED);
        assertThat(Destination.fromRawName("Glarus Nord")).contains(Destination.GLARUS_NORD);
        assertThat(Destination.fromRawName("Buchs (SG)")).contains(Destination.BUCHS_SG);
        assertThat(Destination.fromRawName("Wil (SG)")).contains(Destination.WIL_SG);
        assertThat(Destination.fromRawName("Gossau (SG)")).contains(Destination.GOSSAU_SG);
    }

    @Test
    void normalizesLeadingDestinationBeforeParenthesis() {
        assertThat(Destination.fromRawName("Zurich (Zürich)")).contains(Destination.ZURICH);
        assertThat(Destination.fromRawName("Lucerne (Luzern)")).contains(Destination.LUCERNE);
        assertThat(Destination.fromRawName("Geneva (Genève)")).contains(Destination.GENEVE);
        assertThat(Destination.fromRawName("Ginevra (Genève)")).contains(Destination.GENEVE);
    }

    @Test
    void rejectsParentheticalDescriptionsThatAreNotSameDestinationVariant() {
        assertThat(Destination.fromRawName("Zürich (Hauptstadt der Schweiz)")).isEmpty();
        assertThat(Destination.fromRawName("Lucerne (Chapel Bridge)")).isEmpty();
        assertThat(Destination.fromRawName("Zermatt (Matterhorn)")).isEmpty();
        assertThat(Destination.fromRawName("Interlaken (Berner Oberland)")).isEmpty();
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
        assertThat(Destination.fromRawName("Jungfraujoch")).isEmpty();
        assertThat(Destination.fromRawName("Pilatus")).isEmpty();
        assertThat(Destination.fromRawName("Rigi")).isEmpty();
        assertThat(Destination.fromRawName("Basilia")).isEmpty();
        assertThat(Destination.fromRawName("Bellenz")).isEmpty();
        assertThat(Destination.fromRawName("Tuna")).isEmpty();
        assertThat(Destination.fromRawName("Muerren")).isEmpty();
        assertThat(Destination.fromRawName("Stalden")).isEmpty();
        assertThat(Destination.fromRawName("Bever")).isEmpty();
        assertThat(Destination.fromRawName("Sainte-Croix")).isEmpty();
        assertThat(Destination.fromRawName("Le Locle")).isEmpty();
        assertThat(Destination.fromRawName("Bergün")).isEmpty();
        assertThat(Destination.fromRawName("Glarus")).isEmpty();
    }
}
