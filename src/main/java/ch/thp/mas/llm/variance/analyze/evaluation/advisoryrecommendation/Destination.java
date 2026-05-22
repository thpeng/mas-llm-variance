package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public enum Destination {
    // Source municipality set: https://www.holidaycheck.ch/ferien/schweiz/inspiration/touristenhochburgen
    ZERMATT(Set.of("zermatt")),
    GRINDELWALD(Set.of("grindelwald")),
    MORSCHACH(Set.of("morschach")),
    LAUTERBRUNNEN(Set.of("lauterbrunnen")),
    INTERLAKEN(Set.of("interlaken")),
    LEUKERBAD(Set.of("leukerbad", "loeche-les-bains", "loeche les bains")),
    PONTRESINA(Set.of("pontresina")),
    ST_MORITZ(Set.of("st. moritz", "st moritz", "st-moritz", "saint moritz", "saint-moritz", "sankt moritz")),
    BEATENBERG(Set.of("beatenberg")),
    KANDERSTEG(Set.of("kandersteg")),
    SAAS_FEE(Set.of("saas-fee", "saas fee")),
    AROSA(Set.of("arosa")),
    SAMNAUN(Set.of("samnaun")),
    ANDERMATT(Set.of("andermatt")),
    VAZ_OBERVAZ(Set.of("vaz/obervaz", "vaz-obervaz", "vaz obervaz", "obervaz")),
    VALS(Set.of("vals")),
    HASLIBERG(Set.of("hasliberg")),
    ENGELBERG(Set.of("engelberg")),
    LAAX(Set.of("laax")),
    ASCONA(Set.of("ascona")),
    VITZNAU(Set.of("vitznau")),
    DAVOS(Set.of("davos")),
    CELERINA_SCHLARIGNA(Set.of("celerina/schlarigna", "celerina-schlarigna", "celerina schlarigna", "celerina")),
    LENK(Set.of("lenk")),
    ADELBODEN(Set.of("adelboden")),
    OPFIKON(Set.of("opfikon")),
    MURALTO(Set.of("muralto")),
    FLIMS(Set.of("flims")),
    PARADISO(Set.of("paradiso")),
    WILDERSWIL(Set.of("wilderswil")),
    ZERNEZ(Set.of("zernez")),
    CHAMPERY(Set.of("champery")),
    ANNIVIERS(Set.of("anniviers")),
    WEGGIS(Set.of("weggis")),
    GOMS(Set.of("goms")),
    BRIENZ(Set.of("brienz", "brienz (be)")),
    RHEINWALD(Set.of("rheinwald")),
    SCUOL(Set.of("scuol")),
    HUNDWIL(Set.of("hundwil")),
    SAAS_GRUND(Set.of("saas-grund", "saas grund")),
    VAL_MUESTAIR(Set.of("val mustair", "val muestair")),
    SAANEN(Set.of("saanen")),
    WILDHAUS_ALT_ST_JOHANN(Set.of("wildhaus-alt st. johann", "wildhaus-alt st johann", "wildhaus alt st. johann", "wildhaus alt st johann")),
    LEYSIN(Set.of("leysin")),
    MEYRIN(Set.of("meyrin")),
    INNERTKIRCHEN(Set.of("innertkirchen")),
    KLOTEN(Set.of("kloten")),
    ORSIERES(Set.of("orsieres")),
    UNTERSEEN(Set.of("unterseen")),
    EMMETTEN(Set.of("emmetten")),
    GURTNELLEN(Set.of("gurtnellen")),
    BULLET(Set.of("bullet")),
    BREGAGLIA(Set.of("bregaglia", "bergell")),
    TUJETSCH(Set.of("tujetsch")),
    SURSES(Set.of("surses")),
    ORMONT_DESSUS(Set.of("ormont-dessus", "ormont dessus")),
    SCHANGNAU(Set.of("schangnau")),
    EVOLENE(Set.of("evolene")),
    KLOSTERS(Set.of("klosters")),
    EGERKINGEN(Set.of("egerkingen")),
    POSCHIAVO(Set.of("poschiavo", "puschlaver")),
    SIGRISWIL(Set.of("sigriswil")),
    MEIRINGEN(Set.of("meiringen")),
    QUARTEN(Set.of("quarten")),
    GRUYERES(Set.of("gruyeres", "greyerz", "gruieres")),
    LUCERNE(Set.of("luzern", "lucerne", "lucerna")),
    BAD_RAGAZ(Set.of("bad ragaz")),
    MONTREUX(Set.of("montreux")),
    CRANS_MONTANA(Set.of("crans-montana", "crans montana")),
    LOCARNO(Set.of("locarno")),
    BREIL_BRIGELS(Set.of("breil/brigels", "breil-brigels", "breil brigels", "brigels")),
    AESCHI_BEI_SPIEZ(Set.of("aeschi bei spiez")),
    GONTEN(Set.of("gonten")),
    BRIG_GLIS(Set.of("brig-glis", "brig glis", "brig", "brigue")),
    BECKENRIED(Set.of("beckenried")),
    CLOS_DU_DOUBS(Set.of("clos du doubs")),
    MAYENFELD(Set.of("mayenfeld")),
    SACHSELN(Set.of("sachseln")),
    MATTEN_BEI_INTERLAKEN(Set.of("matten bei interlaken")),
    FEUSISBERG(Set.of("feusisberg")),
    AMDEN(Set.of("amden")),
    SPIEZ(Set.of("spiez")),
    VAL_DE_BAGNES(Set.of("val de bagnes")),
    OLLON(Set.of("ollon")),
    SAIGNELEGIER(Set.of("saignelegier")),
    REICHENBACH_IM_KANDERTAL(Set.of("reichenbach im kandertal")),
    SAMEDAN(Set.of("samedan")),
    GENEVE(Set.of("geneve", "geneva", "genf", "ginevra")),
    APPENZELL(Set.of("appenzell")),
    ZURICH(Set.of("zurich", "zuerich", "zurigo")),
    BERN(Set.of("bern", "berne", "berna")),
    LUGANO(Set.of("lugano")),
    PLAFFEIEN(Set.of("plaffeien")),
    BASEL(Set.of("basel", "bale", "basle", "basilea")),
    VERNIER(Set.of("vernier")),
    INGENBOHL(Set.of("ingenbohl")),
    BOURG_EN_LAVAUX(Set.of("bourg-en-lavaux", "bourg en lavaux")),
    CHUR(Set.of("chur", "coire", "coira", "cuira")),
    LE_CHENIT(Set.of("le chenit")),
    SOLOTHURN(Set.of("solothurn", "soleure", "soletta")),
    LAUSANNE(Set.of("lausanne", "lausana", "losanna")),
    PRATTELN(Set.of("pratteln")),
    HEIDEN(Set.of("heiden")),
    VISP(Set.of("visp", "viege")),
    NENDAZ(Set.of("nendaz")),
    BADEN(Set.of("baden", "baden bei zurich", "baden bei zuerich")),
    GLARUS_SUED(Set.of("glarus sud", "glarus sued")),
    SURSEE(Set.of("sursee")),
    CHATEAU_DOEX(Set.of("chateau-d'oex", "chateau d'oex")),
    ILANZ_GLION(Set.of("ilanz/glion", "ilanz-glion", "ilanz glion", "ilanz")),
    RORSCHACHERBERG(Set.of("rorschacherberg")),
    KRIENS(Set.of("kriens")),
    GAIS(Set.of("gais")),
    VEVEY(Set.of("vevey")),
    CHIASSO(Set.of("chiasso")),
    LUMNEZIA(Set.of("lumnezia")),
    MARTIGNY(Set.of("martigny")),
    SARNEN(Set.of("sarnen")),
    SAINT_MAURICE(Set.of("saint-maurice", "saint maurice", "st. maurice", "st maurice")),
    OLTEN(Set.of("olten")),
    MENDRISIO(Set.of("mendrisio")),
    BLONAY_SAINT_LEGIER(Set.of("blonay - saint-legier", "blonay-saint-legier", "blonay saint-legier", "blonay saint legier")),
    EINSIEDELN(Set.of("einsiedeln")),
    SION(Set.of("sion", "sitten")),
    ST_GALLEN(Set.of("st. gallen", "st.gallen", "st gallen", "saint gallen", "sankt gallen", "san gallo")),
    MURTEN(Set.of("murten", "morat")),
    NEUCHATEL(Set.of("neuchatel", "neuenburg")),
    MORGES(Set.of("morges")),
    DIEMTIGEN(Set.of("diemtigen")),
    FLUMS(Set.of("flums")),
    FREIENBACH(Set.of("freienbach")),
    DELEMONT(Set.of("delemont", "delsberg")),
    NYON(Set.of("nyon")),
    GLARUS_NORD(Set.of("glarus nord")),
    SCHAFFHAUSEN(Set.of("schaffhausen", "schaffhouse", "sciaffusa")),
    ARBON(Set.of("arbon")),
    LANCY(Set.of("lancy")),
    KUESSNACHT_SZ(Set.of("kussnacht (sz)", "kussnacht", "kuessnacht (sz)", "kuessnacht")),
    HAUT_INTYAMON(Set.of("haut-intyamon", "haut intyamon")),
    LENZBURG(Set.of("lenzburg")),
    LANGENTHAL(Set.of("langenthal")),
    UNTERAEGERI(Set.of("unterageri", "unteraegeri")),
    ZUG(Set.of("zug", "zoug", "zugo")),
    BULLE(Set.of("bulle")),
    BELLINZONA(Set.of("bellinzona", "bellinzone")),
    PORRENTRUY(Set.of("porrentruy", "pruntrut")),
    THUN(Set.of("thun", "thoune")),
    YVERDON_LES_BAINS(Set.of("yverdon-les-bains", "yverdon les bains")),
    BUCHS_SG(Set.of("buchs (sg)", "buchs sg", "buchs")),
    BURGDORF(Set.of("burgdorf", "berthoud")),
    WALENSTADT(Set.of("walenstadt")),
    BIEL_BIENNE(Set.of("biel/bienne", "biel-bienne", "biel bienne", "biel", "bienne")),
    NECKERTAL(Set.of("neckertal")),
    BIASCA(Set.of("biasca")),
    FRIBOURG(Set.of("fribourg", "freiburg", "friburgo")),
    MEGGEN(Set.of("meggen")),
    LIESTAL(Set.of("liestal")),
    KREUZLINGEN(Set.of("kreuzlingen")),
    WINTERTHUR(Set.of("winterthur")),
    AARAU(Set.of("aarau")),
    RAPPERSWIL_JONA(Set.of("rapperswil-jona", "rapperswil jona", "rapperswil")),
    ZOFINGEN(Set.of("zofingen")),
    COLLINA_D_ORO(Set.of("collina d'oro", "collina d oro")),
    WIL_SG(Set.of("wil (sg)", "wil sg", "wil")),
    UZWIL(Set.of("uzwil")),
    SCHWYZ(Set.of("schwyz")),
    RHEINFELDEN(Set.of("rheinfelden")),
    TEUFEN_AR(Set.of("teufen (ar)", "teufen ar", "teufen")),
    LA_GRANDE_BEROCHE(Set.of("la grande beroche")),
    SIERRE(Set.of("sierre", "siders")),
    LANGNAU_IM_EMMENTAL(Set.of("langnau im emmental", "langnau")),
    DUEBENDORF(Set.of("dubendorf", "duebendorf")),
    HORGEN(Set.of("horgen")),
    EMMEN(Set.of("emmen")),
    FRAUENFELD(Set.of("frauenfeld")),
    LA_CHAUX_DE_FONDS(Set.of("la chaux-de-fonds", "la chaux de fonds")),
    GRENCHEN(Set.of("grenchen", "granges")),
    ALTSTAETTEN(Set.of("altstatten", "altstaetten")),
    HERZOGENBUCHSEE(Set.of("herzogenbuchsee")),
    USTER(Set.of("uster")),
    MONTECENERI(Set.of("monteceneri")),
    WAEDENSWIL(Set.of("wadenswil", "waedenswil")),
    MOUTIER(Set.of("moutier")),
    KOENIZ(Set.of("koniz", "koeniz")),
    GOSSAU_SG(Set.of("gossau (sg)", "gossau sg", "gossau")),
    ESCHENBACH_SG(Set.of("eschenbach (sg)", "eschenbach sg", "eschenbach"));

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Set<String> variants;

    Destination(Set<String> variants) {
        this.variants = variants;
    }

    public static Optional<Destination> fromRawName(String rawName) {
        Optional<Destination> directMatch = findByNormalizedName(normalize(rawName));
        if (directMatch.isPresent()) {
            return directMatch;
        }

        ParentheticalName parentheticalName = parentheticalName(rawName);
        if (parentheticalName == null) {
            return Optional.empty();
        }
        Optional<Destination> leadingMatch = findByNormalizedName(normalize(parentheticalName.leadingName()));
        Optional<Destination> parentheticalMatch = findByNormalizedName(normalize(parentheticalName.parentheticalName()));
        if (leadingMatch.isPresent() && leadingMatch.equals(parentheticalMatch)) {
            return leadingMatch;
        }
        return Optional.empty();
    }

    private static Optional<Destination> findByNormalizedName(String normalized) {
        return Arrays.stream(values())
                .filter(destination -> destination.variants.stream()
                        .map(Destination::normalize)
                        .anyMatch(normalized::equals))
                .findFirst();
    }

    public static String normalize(String value) {
        String trimmed = EDGE_PUNCTUATION.matcher(value == null ? "" : value.trim()).replaceAll("");
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        String withoutMarkdown = withoutDiacritics.replace("*", "");
        String withoutDots = withoutMarkdown.replace(".", "");
        return WHITESPACE.matcher(withoutDots.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
    }

    private static ParentheticalName parentheticalName(String value) {
        if (value == null) {
            return null;
        }
        int parenthesis = value.indexOf('(');
        int closeParenthesis = value.indexOf(')', parenthesis + 1);
        if (parenthesis < 0 || closeParenthesis < 0) {
            return null;
        }
        return new ParentheticalName(value.substring(0, parenthesis), value.substring(parenthesis + 1, closeParenthesis));
    }

    private record ParentheticalName(String leadingName, String parentheticalName) {
    }
}
