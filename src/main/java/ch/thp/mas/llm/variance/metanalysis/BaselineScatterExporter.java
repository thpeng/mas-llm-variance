package ch.thp.mas.llm.variance.metanalysis;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class BaselineScatterExporter {

    public List<BaselineScatterRow> exportRows(List<MetaAnalysisRow> rows) {
        return rows.stream()
                .filter(row -> "baseline".equals(row.setting()))
                .map(this::exportRow)
                .toList();
    }

    private BaselineScatterRow exportRow(MetaAnalysisRow row) {
        String modelFamily = modelFamily(row.model());
        int modelFamilyId = modelFamilyId(modelFamily);
        int archetypeId = archetypeId(row.archetype());
        return new BaselineScatterRow(
                row.seriesId(),
                row.provider(),
                row.model(),
                modelFamily,
                modelFamilyId,
                row.archetype(),
                archetypeId,
                row.promptLanguage(),
                row.setting(),
                row.nRequested(),
                row.nSuccess(),
                row.literalUniqueCount(),
                share(row.literalUniqueCount(), row.nSuccess()),
                row.semanticValidRate(),
                plotLiteralUniqueCount(row.seriesId(), row.literalUniqueCount(), modelFamilyId, archetypeId),
                plotSemanticValidRate(row.seriesId(), row.semanticValidRate(), modelFamilyId, archetypeId)
        );
    }

    private String modelFamily(String model) {
        if (model == null || model.isBlank()) {
            return "";
        }
        String normalized = model.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("gpt-4o")) {
            return "gpt-4o";
        }
        if (normalized.startsWith("gpt-5.4-mini") || normalized.startsWith("gpt54mini")) {
            return "gpt-5.4-mini";
        }
        if (normalized.contains("claude-sonnet")) {
            return "claude-sonnet";
        }
        if (normalized.contains("gemini")) {
            return "gemini-flash";
        }
        if (normalized.contains("apertus")) {
            return "apertus";
        }
        if (normalized.contains("qwen")) {
            return "qwen";
        }
        if (normalized.contains("gpt-oss")) {
            return "gpt-oss";
        }
        return model;
    }

    private int modelFamilyId(String modelFamily) {
        return switch (modelFamily) {
            case "gpt-4o" -> 1;
            case "gpt-5.4-mini" -> 2;
            case "claude-sonnet" -> 3;
            case "gemini-flash" -> 4;
            case "apertus" -> 5;
            case "qwen" -> 6;
            case "gpt-oss" -> 7;
            default -> 0;
        };
    }

    private int archetypeId(ch.thp.mas.llm.variance.analyze.PromptEvaluation archetype) {
        if (archetype == null) {
            return 0;
        }
        return switch (archetype) {
            case ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP -> 1;
            case FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION -> 2;
            case LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE -> 3;
            case CREATIVE_GENERATIVE_LUCERNE_MARKETING -> 4;
        };
    }

    private Double share(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private Double plotLiteralUniqueCount(String seriesId, int literalUniqueCount, int modelFamilyId, int archetypeId) {
        return literalUniqueCount
                + ((modelFamilyId - 4) * 0.18)
                + ((archetypeId - 2.5) * 0.08)
                + seriesXOffset(seriesId)
                + cornerXOffset(seriesId, literalUniqueCount);
    }

    private Double plotSemanticValidRate(String seriesId, Double semanticValidRate, int modelFamilyId, int archetypeId) {
        if (semanticValidRate == null) {
            return null;
        }
        return semanticValidRate
                + ((modelFamilyId - 4) * 0.0025)
                + ((archetypeId - 2.5) * 0.006)
                + seriesYOffset(seriesId)
                + cornerYOffset(seriesId, semanticValidRate);
    }

    private double seriesXOffset(String seriesId) {
        int number = seriesNumber(seriesId);
        return ((number % 11) - 5) * 0.035;
    }

    private double seriesYOffset(String seriesId) {
        int number = seriesNumber(seriesId);
        return ((number % 13) - 6) * 0.0015;
    }

    private double cornerXOffset(String seriesId, int literalUniqueCount) {
        if (literalUniqueCount > 2) {
            return 0.0;
        }
        int number = seriesNumber(seriesId);
        return ((number % 7) - 3) * 0.28;
    }

    private double cornerYOffset(String seriesId, double semanticValidRate) {
        if (semanticValidRate > 0.01 && semanticValidRate < 0.99) {
            return 0.0;
        }
        int number = seriesNumber(seriesId);
        return ((number % 9) - 4) * 0.01;
    }

    private int seriesNumber(String seriesId) {
        if (seriesId == null || seriesId.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(seriesId.substring(0, 4));
        } catch (NumberFormatException ignored) {
            return Math.abs(seriesId.hashCode());
        }
    }
}
