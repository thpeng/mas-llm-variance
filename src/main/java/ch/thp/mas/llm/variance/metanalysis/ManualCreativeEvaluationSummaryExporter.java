package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextExtraction;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManualCreativeEvaluationSummaryExporter {

    private static final Path DEFAULT_MANUAL_SAMPLE = Path.of(
            "src",
            "main",
            "resources",
            "analysis",
            "manual_review",
            "1007-main-manual-evaluation-creative-sample.json"
    );

    private final Function<String, NamedRunLog> runLogReader;
    private final ObjectMapper objectMapper;

    @Autowired
    public ManualCreativeEvaluationSummaryExporter(RunLogReader runLogReader) {
        this(runLogReader::read, defaultObjectMapper());
    }

    ManualCreativeEvaluationSummaryExporter(Function<String, NamedRunLog> runLogReader, ObjectMapper objectMapper) {
        this.runLogReader = runLogReader;
        this.objectMapper = objectMapper;
    }

    public List<ManualCreativeEvaluationSummaryRow> exportRows(List<NamedAnalysisResult> analyses) {
        Map<String, ManualItem> manualItems = readManualItems(DEFAULT_MANUAL_SAMPLE);
        Map<String, MutableRow> rowsBySeries = new LinkedHashMap<>();

        for (NamedAnalysisResult namedAnalysis : analyses) {
            AnalysisResult analysis = namedAnalysis.analysisResult();
            if (analysis.config().promptEvaluation() != PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING
                    || analysis.lucerneMarketingText() == null) {
                continue;
            }
            RunLog runLog = runLogReader.apply(analysis.sourceRun()).runLog();
            MutableRow row = new MutableRow(runLog.planName(), runLog.model(), setting(runLog.planName()));
            Map<Integer, RunLogEntry> entries = new LinkedHashMap<>();
            for (RunLogEntry entry : runLog.repetitions()) {
                entries.put(entry.index(), entry);
            }
            Map<Integer, LucerneMarketingTextExtraction> extractions = new LinkedHashMap<>();
            for (LucerneMarketingTextExtraction extraction : analysis.lucerneMarketingText().extractions()) {
                extractions.put(extraction.responseIndex(), extraction);
            }
            for (LucerneMarketingTextExtraction extraction : extractions.values()) {
                String id = ManualEvaluationId.id(runLog.planName(), extraction.responseIndex());
                ManualItem manual = manualItems.get(id);
                if (manual == null) {
                    continue;
                }
                row.sampleSize++;
                RunLogEntry entry = entries.get(extraction.responseIndex());
                if (entry != null) {
                    row.literalResponses.add(nullToEmpty(entry.response()));
                }
                if (extraction.containsRequiredTerm()) {
                    row.analysisLucerneFoundCount++;
                }
                if (extraction.sentenceCount() == extraction.expectedSentenceCount()) {
                    row.analysisThreeSentencesCount++;
                }
                if (Boolean.TRUE.equals(manual.tourismusbezug())) {
                    row.manualTourismReferenceCount++;
                }
                if (Boolean.TRUE.equals(manual.luzernbezug())) {
                    row.manualPlaceReferenceCount++;
                }
                if ("Ja".equalsIgnoreCase(nullToEmpty(manual.halluzination()).trim())) {
                    row.manualHallucinationCount++;
                }
            }
            if (row.sampleSize > 0) {
                rowsBySeries.put(row.seriesId, row);
            }
        }

        return rowsBySeries.values().stream()
                .map(MutableRow::toRow)
                .sorted(Comparator.comparing(ManualCreativeEvaluationSummaryRow::seriesId))
                .toList();
    }

    private Map<String, ManualItem> readManualItems(Path path) {
        try {
            JsonNode root = objectMapper.readTree(stripBom(Files.readString(path)));
            Map<String, ManualItem> items = new LinkedHashMap<>();
            for (JsonNode item : root.path("items")) {
                JsonNode evaluation = item.path("evaluation");
                String id = item.path("id").asText();
                items.put(id, new ManualItem(
                        id,
                        booleanOrNull(evaluation.get("tourismusbezug")),
                        booleanOrNull(evaluation.get("luzernbezug")),
                        textOrNull(evaluation.get("halluzination"))
                ));
            }
            return items;
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not read manual creative evaluation sample: " + path, e);
        }
    }

    private Boolean booleanOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        String text = node.asText();
        if ("true".equalsIgnoreCase(text) || "y".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "n".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private String setting(String planName) {
        String normalized = nullToEmpty(planName).toLowerCase(Locale.ROOT);
        if (normalized.contains("-hoch")) {
            return "hoch";
        }
        if (normalized.contains("-mittel")) {
            return "mittel";
        }
        return "baseline";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private record ManualItem(
            String id,
            Boolean tourismusbezug,
            Boolean luzernbezug,
            String halluzination
    ) {
    }

    private static class MutableRow {

        private final String seriesId;
        private final String model;
        private final String setting;
        private final Set<String> literalResponses = new HashSet<>();
        private int sampleSize;
        private int analysisLucerneFoundCount;
        private int analysisThreeSentencesCount;
        private int manualTourismReferenceCount;
        private int manualPlaceReferenceCount;
        private int manualHallucinationCount;

        private MutableRow(String seriesId, String model, String setting) {
            this.seriesId = seriesId;
            this.model = model;
            this.setting = setting;
        }

        private ManualCreativeEvaluationSummaryRow toRow() {
            return new ManualCreativeEvaluationSummaryRow(
                    seriesId,
                    model,
                    setting,
                    sampleSize,
                    literalResponses.size(),
                    analysisLucerneFoundCount,
                    analysisThreeSentencesCount,
                    manualTourismReferenceCount,
                    manualPlaceReferenceCount,
                    manualHallucinationCount
            );
        }
    }
}
