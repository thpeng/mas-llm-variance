package ch.thp.mas.llm.variance.metanalysis;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class MetaAnalysisCommand {

    private final AnalysisResultReader analysisResultReader;
    private final MetaAnalysisExporter metaAnalysisExporter;
    private final MetaAnalysisCsvWriter csvWriter;
    private final WordDriftAnalysisExporter wordDriftAnalysisExporter;
    private final WordDriftCsvWriter wordDriftCsvWriter;
    private final BaselineScatterExporter baselineScatterExporter;
    private final BaselineScatterCsvWriter baselineScatterCsvWriter;
    private final CreativeControlQuantileExporter creativeControlQuantileExporter;
    private final CreativeControlQuantileCsvWriter creativeControlQuantileCsvWriter;
    private final RoundTripLanguageDestinationExporter roundTripLanguageDestinationExporter;
    private final RoundTripLanguageDestinationCsvWriter roundTripLanguageDestinationCsvWriter;

    public MetaAnalysisCommand(
            AnalysisResultReader analysisResultReader,
            MetaAnalysisExporter metaAnalysisExporter,
            MetaAnalysisCsvWriter csvWriter,
            WordDriftAnalysisExporter wordDriftAnalysisExporter,
            WordDriftCsvWriter wordDriftCsvWriter,
            BaselineScatterExporter baselineScatterExporter,
            BaselineScatterCsvWriter baselineScatterCsvWriter,
            CreativeControlQuantileExporter creativeControlQuantileExporter,
            CreativeControlQuantileCsvWriter creativeControlQuantileCsvWriter,
            RoundTripLanguageDestinationExporter roundTripLanguageDestinationExporter,
            RoundTripLanguageDestinationCsvWriter roundTripLanguageDestinationCsvWriter
    ) {
        this.analysisResultReader = analysisResultReader;
        this.metaAnalysisExporter = metaAnalysisExporter;
        this.csvWriter = csvWriter;
        this.wordDriftAnalysisExporter = wordDriftAnalysisExporter;
        this.wordDriftCsvWriter = wordDriftCsvWriter;
        this.baselineScatterExporter = baselineScatterExporter;
        this.baselineScatterCsvWriter = baselineScatterCsvWriter;
        this.creativeControlQuantileExporter = creativeControlQuantileExporter;
        this.creativeControlQuantileCsvWriter = creativeControlQuantileCsvWriter;
        this.roundTripLanguageDestinationExporter = roundTripLanguageDestinationExporter;
        this.roundTripLanguageDestinationCsvWriter = roundTripLanguageDestinationCsvWriter;
    }

    public Path run(ApplicationArguments appArgs) {
        String selection = optionValue(appArgs, "metanalysis");
        List<NamedAnalysisResult> analyses = analysisResultReader.readSelection(selection);
        return switch (kind(appArgs)) {
            case SUMMARY -> {
                List<MetaAnalysisRow> rows = metaAnalysisExporter.exportRows(analyses);
                yield csvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
            }
            case WORD_DRIFT -> {
                List<WordDriftRow> rows = wordDriftAnalysisExporter.exportRows(analyses);
                yield wordDriftCsvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
            }
            case BASELINE_SCATTER -> {
                List<MetaAnalysisRow> summaryRows = metaAnalysisExporter.exportRows(analyses);
                List<BaselineScatterRow> rows = baselineScatterExporter.exportRows(summaryRows);
                yield baselineScatterCsvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
            }
            case CREATIVE_CONTROL_QUANTILES -> {
                List<CreativeControlQuantileRow> rows = creativeControlQuantileExporter.exportRows(analyses);
                yield creativeControlQuantileCsvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
            }
            case ROUNDTRIP_LANGUAGE_DESTINATION -> {
                RoundTripLanguageDestinationExport export = roundTripLanguageDestinationExporter.exportRows(analyses);
                yield roundTripLanguageDestinationCsvWriter.write(export, selection, optionalValue(appArgs, "metanalysis-output"));
            }
        };
    }

    private MetaAnalysisKind kind(ApplicationArguments appArgs) {
        String value = optionalValue(appArgs, "metanalysis-kind");
        if (value == null) {
            value = optionalValue(appArgs, "metanalysis-type");
        }
        if (value == null) {
            return MetaAnalysisKind.SUMMARY;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT);
        try {
            return MetaAnalysisKind.valueOf(normalized);
        } catch (Exception e) {
            throw new MetaAnalysisException("Unknown metanalysis kind: " + value, e);
        }
    }

    private String optionValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new MetaAnalysisException("--" + name + " must specify a value.");
        }
        return values.getFirst();
    }

    private String optionalValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            return null;
        }
        return values.getFirst();
    }
}
