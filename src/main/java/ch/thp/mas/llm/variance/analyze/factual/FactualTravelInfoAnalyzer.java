package ch.thp.mas.llm.variance.analyze.factual;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FactualTravelInfoAnalyzer {

    private static final Pattern TIME = Pattern.compile("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b");
    private static final Pattern ZERO_CHANGES = Pattern.compile(
            "(?iu)\\b(?:0\\s*(?:umstieg|umstiege|changes|transfers|cambi)"
                    + "|null\\s*umstiege?"
                    + "|umstiege?:\\s*keine"
                    + "|umstiege?:\\s*0"
                    + "|keine\\s+umstiege"
                    + "|kein\\s+(?:umstieg|umsteigen)"
                    + "|ohne\\s+(?:umstieg|umstiege|umzusteigen)"
                    + "|umstiegsfrei"
                    + "|direkte\\s+verbindung"
                    + "|direktverbindung"
                    + "|direkt"
                    + "|no\\s+(?:changes|transfers)"
                    + "|aucun\\s+changement"
                    + "|aucune\\s+correspondance"
                    + "|senza\\s+cambi)\\b");

    public FactualTravelInfoAnalysis analyze(
            List<String> responses,
            FactualTravelInfoConfig config,
            FactualSyntacticAnalysisFactory syntacticFactory
    ) {
        List<FactualTravelInfoExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config));
        }

        List<Integer> successIndices = extractions.stream()
                .filter(extraction -> extraction.status() == FactualTravelInfoStatus.SUCCESS)
                .map(FactualTravelInfoExtraction::responseIndex)
                .toList();
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.status() == FactualTravelInfoStatus.OUTLIER)
                .map(FactualTravelInfoExtraction::responseIndex)
                .toList();

        return new FactualTravelInfoAnalysis(
                responses.size(),
                successIndices.size(),
                outliers.size(),
                responses.isEmpty() ? 0.0 : (double) successIndices.size() / responses.size(),
                outliers,
                count(extractions, FactualTravelInfoExtraction::departureFound),
                count(extractions, FactualTravelInfoExtraction::arrivalFound),
                count(extractions, FactualTravelInfoExtraction::changesFound),
                extraTimeCounts(extractions),
                extractions,
                syntacticFactory.create(successIndices)
        );
    }

    FactualTravelInfoExtraction extract(int responseIndex, String response, FactualTravelInfoConfig config) {
        List<String> normalizedTimes = normalizedTimes(response);
        boolean departureFound = normalizedTimes.contains(config.departureFromBern());
        boolean arrivalFound = normalizedTimes.contains(config.arrivalAtZurich());
        List<String> extraTimes = normalizedTimes.stream()
                .filter(time -> !time.equals(config.departureFromBern()))
                .filter(time -> !time.equals(config.arrivalAtZurich()))
                .toList();
        Matcher changesMatcher = ZERO_CHANGES.matcher(response);
        boolean changesFound = changesMatcher.find();
        String detectedChangeExpression = changesFound ? changesMatcher.group() : null;

        List<String> failureReasons = new ArrayList<>();
        if (!departureFound) {
            failureReasons.add("departure_missing");
        }
        if (!arrivalFound) {
            failureReasons.add("arrival_missing");
        }
        if (!changesFound) {
            failureReasons.add("changes_missing");
        }
        FactualTravelInfoStatus status = failureReasons.isEmpty()
                ? FactualTravelInfoStatus.SUCCESS
                : FactualTravelInfoStatus.OUTLIER;

        return new FactualTravelInfoExtraction(
                responseIndex,
                response,
                normalizedTimes,
                extraTimes,
                departureFound,
                arrivalFound,
                changesFound,
                changesFound ? config.changes() : null,
                detectedChangeExpression,
                status,
                List.copyOf(failureReasons)
        );
    }

    List<String> normalizedTimes(String response) {
        List<String> times = new ArrayList<>();
        Matcher matcher = TIME.matcher(response);
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            times.add("%02d:%02d".formatted(hour, minute));
        }
        return times;
    }

    private Map<String, Integer> extraTimeCounts(List<FactualTravelInfoExtraction> extractions) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FactualTravelInfoExtraction extraction : extractions) {
            for (String extraTime : extraction.extraTimes()) {
                counts.merge(extraTime, 1, Integer::sum);
            }
        }
        return counts;
    }

    private int count(
            List<FactualTravelInfoExtraction> extractions,
            java.util.function.Predicate<FactualTravelInfoExtraction> predicate
    ) {
        return (int) extractions.stream().filter(predicate).count();
    }

    @FunctionalInterface
    public interface FactualSyntacticAnalysisFactory {
        ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis create(List<Integer> successIndices);
    }
}
