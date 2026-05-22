package ch.thp.mas.llm.variance.client;

public class ServingException extends Exception {

    private final int statusCode;
    private final String responseBody;
    private final RequestTrace requestTrace;

    public ServingException(String message, int statusCode, String responseBody, RequestTrace requestTrace) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.requestTrace = requestTrace;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    public RequestTrace requestTrace() {
        return requestTrace;
    }

    public boolean isServingError() {
        return statusCode == 200 || statusCode >= 500 && statusCode <= 599;
    }
}
