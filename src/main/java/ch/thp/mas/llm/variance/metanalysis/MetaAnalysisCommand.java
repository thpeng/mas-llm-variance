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
    private final FirstResponseEffectExporter firstResponseEffectExporter;
    private final FirstResponseEffectCsvWriter firstResponseEffectCsvWriter;
    private final RoundTripStationModelCountExporter roundTripStationModelCountExporter;
    private final RoundTripStationModelCountCsvWriter roundTripStationModelCountCsvWriter;
    private final RoundTripRouteModelCountExporter roundTripRouteModelCountExporter;
    private final RoundTripRouteModelCountCsvWriter roundTripRouteModelCountCsvWriter;

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
            RoundTripLanguageDestinationCsvWriter roundTripLanguageDestinationCsvWriter,
            RoundTripStationModelCountExporter roundTripStationModelCountExporter,
            RoundTripStationModelCountCsvWriter roundTripStationModelCountCsvWriter,
            RoundTripRouteModelCountExporter roundTripRouteModelCountExporter,
            RoundTripRouteModelCountCsvWriter roundTripRouteModelCountCsvWriter
            RoundTripLanguageDestinationCsvWriter roundTripLanguageDestinationCsvWriter,
            FirstResponseEffectExporter firstResponseEffectExporter,
            FirstResponseEffectCsvWriter firstResponseEffectCsvWriter
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
        this.firstResponseEffectExporter = firstResponseEffectExporter;
        this.firstResponseEffectCsvWriter = firstResponseEffectCsvWriter;
        this.roundTripStationModelCountExporter = roundTripStationModelCountExporter;
        this.roundTripStationModelCountCsvWriter = roundTripStationModelCountCsvWriter;
        this.roundTripRouteModelCountExporter = roundTripRouteModelCountExporter;
        this.roundTripRouteModelCountCsvWriter = roundTripRouteModelCountCsvWriter;
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
            case FIRST_RESPONSE_EFFECT -> {
                List<FirstResponseEffectRow> rows = firstResponseEffectExporter.exportRows(analyses);
                yield firstResponseEffectCsvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
            }
            case ROUNDTRIP_STATION_MODEL_COUNTS -> {
                List<RoundTripStationModelCountRow> rows = roundTripStationModelCountExporter.exportRows(analyses);
                yield roundTripStationModelCountCsvWriter.write(rows, optionalValue(appArgs, "metanalysis-output"));
            }
            case ROUNDTRIP_ROUTE_MODEL_COUNTS -> {
                List<RoundTripRouteModelCountRow> rows = roundTripRouteModelCountExporter.exportRows(analyses);
                yield roundTripRouteModelCountCsvWriter.write(rows, optionalValue(appArgs, "metanalysis-output"));
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
