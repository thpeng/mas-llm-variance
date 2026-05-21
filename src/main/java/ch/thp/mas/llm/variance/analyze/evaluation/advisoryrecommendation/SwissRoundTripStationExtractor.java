package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SwissRoundTripStationExtractor {

    private static final Pattern NUMBERED_LINE = Pattern.compile("^\\s*#*\\s*\\d+[.)]\\s*(.+?)\\s*$");
    private static final Pattern BOLD_NUMBERED_LINE = Pattern.compile("^\\s*\\*\\*\\s*\\d+[.)]\\s*(.+?)\\s*\\*\\*\\s*$");
    private static final Pattern BOLD_NAME = Pattern.compile("\\*\\*\\s*(.+?)\\s*\\*\\*");
    private static final Pattern EDGE_MARKDOWN = Pattern.compile("^[\\s*_`]+|[\\s*_`,.;:!?-]+$");

    public List<String> extract(String response) {
        List<String> names = new ArrayList<>();
        for (String line : response.split("\\R")) {
            Matcher lineMatcher = NUMBERED_LINE.matcher(line);
            Matcher boldLineMatcher = BOLD_NUMBERED_LINE.matcher(line);
            if (lineMatcher.matches()) {
                addName(names, lineMatcher.group(1));
            } else if (boldLineMatcher.matches()) {
                addName(names, boldLineMatcher.group(1));
            }
        }
        return names;
    }

    private void addName(List<String> names, String item) {
        String name = extractName(item);
        if (!name.isBlank()) {
            names.add(name);
        }
    }

    private String extractName(String item) {
        Matcher boldMatcher = BOLD_NAME.matcher(item);
        if (boldMatcher.find()) {
            return clean(boldMatcher.group(1));
        }
        int delimiter = firstDelimiter(item);
        String candidate = delimiter >= 0 ? item.substring(0, delimiter) : item;
        return clean(candidate);
    }

    private int firstDelimiter(String item) {
        return java.util.stream.IntStream.of(
                        item.indexOf(':'),
                        item.indexOf('-'),
                        item.indexOf('\u2013'),
                        item.indexOf('\u2014'),
                        item.indexOf('(')
                )
                .filter(index -> index >= 0)
                .min()
                .orElse(-1);
    }

    private String clean(String value) {
        return EDGE_MARKDOWN.matcher(value.trim().replace("**", "")).replaceAll("").trim();
    }
}
