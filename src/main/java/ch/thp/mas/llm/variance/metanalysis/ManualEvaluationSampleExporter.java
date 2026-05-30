package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.RunLogEntryStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManualEvaluationSampleExporter {

    static final long SAMPLE_SEED = 20260529L;
    private static final String SCHEMA = "manual-hallucination-evaluation-sample-v1";
    private static final int WRAP_AT = 100;
    private static final Map<String, Integer> SAMPLE_SIZES = sampleSizes();

    private final Function<String, NamedRunLog> runLogReader;

    @Autowired
    public ManualEvaluationSampleExporter(RunLogReader runLogReader) {
        this(runLogReader::read);
    }

    ManualEvaluationSampleExporter(Function<String, NamedRunLog> runLogReader) {
        this.runLogReader = runLogReader;
    }

    public ManualEvaluationSampleExport exportSample(List<NamedAnalysisResult> analyses) {
        List<SampleCandidate> candidates = analyses.stream()
                .map(this::candidate)
                .filter(candidate -> SAMPLE_SIZES.containsKey(candidate.seriesId()))
                .sorted(Comparator.comparing(SampleCandidate::seriesId))
                .toList();

        ManualEvaluationSample roundTrip = buildSample(
                candidates,
                PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP,
                "1007-main-manual-evaluation-roundtrip-sample",
                "Blind bewerten. Die ID ist deterministisch aus Versuchsreihe und Wiederholungsindex abgeleitet; "
                        + "das Datenprodukt enthaelt bewusst keine Modell- oder Reihenangaben. "
                        + "Fuer Rundreisen nur halluzination mit Nein, Ja oder nicht bestimmbar eintragen.",
                Map.of("halluzination", List.of("Nein", "Ja", "nicht bestimmbar"))
        );
        ManualEvaluationSample creative = buildSample(
                candidates,
                PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING,
                "1007-main-manual-evaluation-creative-sample",
                "Blind bewerten. Die ID ist deterministisch aus Versuchsreihe und Wiederholungsindex abgeleitet; "
                        + "das Datenprodukt enthaelt bewusst keine Modell- oder Reihenangaben. "
                        + "Fuer kreative Luzern-Texte tourismusbezug und luzernbezug mit true/false sowie "
                        + "halluzination mit Nein, Ja oder nicht bestimmbar eintragen.",
                Map.of(
                        "tourismusbezug", List.of("true", "false"),
                        "luzernbezug", List.of("true", "false"),
                        "halluzination", List.of("Nein", "Ja", "nicht bestimmbar")
                )
        );
        return new ManualEvaluationSampleExport(roundTrip, creative);
    }

    private SampleCandidate candidate(NamedAnalysisResult namedAnalysis) {
        AnalysisResult analysis = namedAnalysis.analysisResult();
        RunLog runLog = runLogReader.apply(analysis.sourceRun()).runLog();
        return new SampleCandidate(runLog.planName(), analysis.config().promptEvaluation(), runLog);
    }

    private ManualEvaluationSample buildSample(
            List<SampleCandidate> candidates,
            PromptEvaluation promptEvaluation,
            String sampleId,
            String instructions,
            Map<String, List<String>> allowedValues
    ) {
        List<ManualEvaluationSampleItem> selected = new ArrayList<>();
        for (SampleCandidate candidate : candidates) {
            if (candidate.promptEvaluation() == promptEvaluation) {
                selected.addAll(sample(candidate));
            }
        }
        Collections.shuffle(selected, new Random(SAMPLE_SEED + promptEvaluation.ordinal()));

        List<ManualEvaluationSampleItem> numbered = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            ManualEvaluationSampleItem item = selected.get(i);
            numbered.add(new ManualEvaluationSampleItem(
                    item.id(),
                    i + 1,
                    item.responseLines(),
                    item.evaluation()
            ));
        }

        return new ManualEvaluationSample(
                SCHEMA,
                sampleId,
                SAMPLE_SEED,
                instructions,
                allowedValues,
                numbered.size(),
                numbered
        );
    }

    private List<ManualEvaluationSampleItem> sample(SampleCandidate candidate) {
        List<RunLogEntry> successfulEntries = candidate.runLog().repetitions().stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SUCCESS)
                .sorted(Comparator.comparingInt(RunLogEntry::index))
                .toList();
        int sampleSize = SAMPLE_SIZES.get(candidate.seriesId());
        List<RunLogEntry> entries = new ArrayList<>(successfulEntries);
        if (sampleSize < entries.size()) {
            Collections.shuffle(entries, new Random(seriesSeed(candidate.seriesId())));
            entries = entries.subList(0, sampleSize);
        }
        return entries.stream()
                .sorted(Comparator.comparingInt(RunLogEntry::index))
                .map(entry -> item(candidate.seriesId(), entry))
                .toList();
    }

    private ManualEvaluationSampleItem item(String seriesId, RunLogEntry entry) {
        String response = sanitizeBlindText(entry.response() == null ? "" : entry.response());
        return new ManualEvaluationSampleItem(
                id(seriesId, entry.index()),
                0,
                wrap(response),
                evaluationFields(seriesId)
        );
    }

    private String sanitizeBlindText(String text) {
        return text
                .replaceAll("(?i)\\banthropic\\b", "[REDACTED]")
                .replaceAll("(?i)\\bclaude\\b", "[REDACTED]")
                .replaceAll("(?i)\\bopenai\\b", "[REDACTED]")
                .replaceAll("(?i)\\bgpt[- ]?oss\\b", "[REDACTED]")
                .replaceAll("(?i)\\bgpt\\b", "[REDACTED]")
                .replaceAll("(?i)\\bgemini\\b", "[REDACTED]")
                .replaceAll("(?i)\\bqwen\\b", "[REDACTED]")
                .replaceAll("(?i)\\bapertus\\b", "[REDACTED]")
                .replaceAll("(?i)\\blm\\s*studio\\b", "[REDACTED]");
    }

    private Object evaluationFields(String seriesId) {
        if (seriesId.contains("-creative-")) {
            return ManualCreativeEvaluationFields.empty();
        }
        return ManualRoundTripEvaluationFields.empty();
    }

    private List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        String[] rawLines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String rawLine : rawLines) {
            if (rawLine.isBlank()) {
                lines.add("");
                continue;
            }
            String remaining = rawLine.stripTrailing();
            while (remaining.length() > WRAP_AT) {
                int cut = remaining.lastIndexOf(' ', WRAP_AT);
                if (cut < WRAP_AT / 2) {
                    cut = WRAP_AT;
                }
                lines.add(remaining.substring(0, cut).stripTrailing());
                remaining = remaining.substring(cut).stripLeading();
            }
            lines.add(remaining);
        }
        return lines;
    }

    private String id(String seriesId, int index) {
        return "bms-" + letterHash(SCHEMA + "|" + SAMPLE_SEED + "|" + seriesId + "|" + index, 24);
    }

    private long seriesSeed(String seriesId) {
        long seed = SAMPLE_SEED;
        for (byte b : seriesId.getBytes(StandardCharsets.UTF_8)) {
            seed = seed * 31 + b;
        }
        return seed;
    }

    private String letterHash(String value, int length) {
        try {
            String alphabet = "abcdef";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(length);
            int index = 0;
            while (builder.length() < length) {
                int valueByte = hash[index % hash.length] & 0xff;
                builder.append(alphabet.charAt(valueByte % alphabet.length()));
                index++;
            }
            return builder.toString();
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not hash manual evaluation sample id.", e);
        }
    }

    private static Map<String, Integer> sampleSizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("0107-anthropic-sonnet46-creative-baseline", 20);
        sizes.put("0201-google-gemini35flash-roundtrip-de-mittel", 20);
        sizes.put("0209-google-gemini35flash-creative-baseline", 20);
        sizes.put("0005-openai-gpt54mini-roundtrip-de-mittel", 20);
        sizes.put("0020-openai-gpt54mini-creative-baseline", 20);
        sizes.put("0023-openai-gpt54mini-creative-mittel", 20);
        sizes.put("0024-openai-gpt54mini-creative-hoch", 50);
        sizes.put("0501-lmstudio-gptoss20b-roundtrip-de-reasoning-low-mittel", 50);
        sizes.put("0507-lmstudio-gptoss20b-creative-reasoning-low-baseline", 20);
        sizes.put("0401-lmstudio-qwen35-9b-roundtrip-de-mittel", 100);
        sizes.put("0408-lmstudio-qwen35-9b-creative-baseline", 20);
        sizes.put("0301-lmstudio-apertus-roundtrip-de-mittel", 100);
        sizes.put("0307-lmstudio-apertus-creative-baseline", 20);
        sizes.put("0310-lmstudio-apertus-creative-mittel", 50);
        sizes.put("0311-lmstudio-apertus-creative-hoch", 100);
        return Map.copyOf(sizes);
    }

    private record SampleCandidate(String seriesId, PromptEvaluation promptEvaluation, RunLog runLog) {
    }
}
