package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public enum Destination {
    GENEVE(Set.of("geneve", "genève", "geneva", "genf", "ginevra")),
    ZURICH(Set.of("zurich", "zürich", "zuerich", "zurigo")),
    LUCERNE(Set.of("lucerne", "luzern", "lucerna")),
    ZERMATT(Set.of("zermatt")),
    INTERLAKEN(Set.of("interlaken")),
    MONTREUX(Set.of("montreux")),
    LUGANO(Set.of("lugano")),
    ST_MORITZ(Set.of("st. moritz", "st moritz", "st-moritz", "saint moritz", "saint-moritz", "sankt moritz")),
    BERN(Set.of("bern", "berne", "berna")),
    BASEL(Set.of("basel", "bâle", "basle", "basilea")),
    CHUR(Set.of("chur", "coire", "coira", "cuira")),
    LAUSANNE(Set.of("lausanne", "lausana", "losanna")),
    LOCARNO(Set.of("locarno")),
    GRINDELWALD(Set.of("grindelwald")),
    LAUTERBRUNNEN(Set.of("lauterbrunnen")),
    WENGEN(Set.of("wengen")),
    MUERREN(Set.of("mürren", "muerren", "murren")),
    DAVOS(Set.of("davos")),
    AROSA(Set.of("arosa")),
    GSTAAD(Set.of("gstaad")),
    SAAS_FEE(Set.of("saas-fee", "saas fee")),
    VERBIER(Set.of("verbier")),
    ANDERMATT(Set.of("andermatt")),
    ASCONA(Set.of("ascona")),
    BELLINZONA(Set.of("bellinzona", "bellinzone")),
    SCHAFFHAUSEN(Set.of("schaffhausen", "schaffhouse", "sciaffusa")),
    ST_GALLEN(Set.of("st. gallen", "st.gallen", "st gallen", "saint gallen", "sankt gallen", "san gallo")),
    APPENZELL(Set.of("appenzell")),
    THUN(Set.of("thun", "thoune")),
    BRIENZ(Set.of("brienz")),
    GRUYERES(Set.of("gruyères", "gruyeres", "greyerz", "gruieres")),
    FRIBOURG(Set.of("fribourg", "freiburg", "friburgo")),
    NEUCHATEL(Set.of("neuchâtel", "neuchatel", "neuenburg")),
    SION(Set.of("sion", "sitten")),
    VEVEY(Set.of("vevey")),
    SOLOTHURN(Set.of("solothurn", "soleure", "soletta")),
    ZUG(Set.of("zug", "zoug", "zugo")),
    RAPPERSWIL(Set.of("rapperswil")),
    BADEN(Set.of("baden", "baden bei zürich", "baden bei zuerich")),
    SAANEN(Set.of("saanen")),
    BIEL_BIENNE(Set.of("biel/bienne", "biel", "bienne")),
    STALDEN(Set.of("stalden")),
    BEVER(Set.of("bever")),
    ENGELBERG(Set.of("engelberg")),
    BRIG(Set.of("brig", "brigue")),
    SAINTE_CROIX(Set.of("sainte-croix", "sainte croix")),
    LE_LOCLE(Set.of("le locle")),
    BERGUEN(Set.of("bergün", "berguen", "bergun")),
    BULLE(Set.of("bulle")),
    GLARUS(Set.of("glarus")),
    BURGDORF(Set.of("burgdorf", "berthoud"));

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
