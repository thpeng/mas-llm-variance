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

    public MetaAnalysisCommand(
            AnalysisResultReader analysisResultReader,
            MetaAnalysisExporter metaAnalysisExporter,
            MetaAnalysisCsvWriter csvWriter
    ) {
        this.analysisResultReader = analysisResultReader;
        this.metaAnalysisExporter = metaAnalysisExporter;
        this.csvWriter = csvWriter;
    }

    public Path run(ApplicationArguments appArgs) {
        String selection = optionValue(appArgs, "metanalysis");
        List<NamedAnalysisResult> analyses = analysisResultReader.readSelection(selection);
        List<MetaAnalysisRow> rows = metaAnalysisExporter.exportRows(analyses);
        return csvWriter.write(rows, selection, optionalValue(appArgs, "metanalysis-output"));
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
