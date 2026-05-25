package ch.thp.mas.llm.variance.metanalysis;

public class MetaAnalysisException extends RuntimeException {

    public MetaAnalysisException(String message) {
        super(message);
    }

    public MetaAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
