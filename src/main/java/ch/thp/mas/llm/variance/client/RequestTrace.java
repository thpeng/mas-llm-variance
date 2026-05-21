package ch.thp.mas.llm.variance.client;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record RequestTrace(
        String url,
        Map<String, List<String>> headers,
        String body,
        Integer responseStatusCode,
        Map<String, List<String>> responseHeaders,
        String responseBody
) {

    private static final List<String> AUTHENTICATION_HEADER_NAMES = List.of(
            "authorization",
            "x-api-key",
            "x-goog-api-key",
            "api-key",
            "anthropic-api-key"
    );
    private static final List<String> SENSITIVE_HEADER_NAMES = List.of(
            "cookie",
            "set-cookie",
            "openai-organization",
            "openai-project",
            "x-request-id",
            "request-id",
            "cf-ray",
            "traceparent",
            "tracestate",
            "traceresponse",
            "anthropic-organization-id",
            "x-trace-id",
            "x-correlation-id"
    );
    private static final List<String> REDACTED_VALUE = List.of("<redacted>");
    private static final Pattern SENSITIVE_JSON_STRING_FIELDS = Pattern.compile(
            "(\"(?:signature|thoughtSignature|responseId|id)\"\\s*:\\s*\")((?:\\\\.|[^\"\\\\])*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    public RequestTrace {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        responseHeaders = responseHeaders == null ? Map.of() : Map.copyOf(responseHeaders);
    }

    public static RequestTrace of(HttpRequest request) {
        return of(request, null, null);
    }

    public static RequestTrace of(HttpRequest request, String requestBody, HttpResponse<String> response) {
        return new RequestTrace(
                sanitizeUrl(request.uri()),
                sanitizeHeaders(request.headers().map()),
                requestBody,
                response == null ? null : response.statusCode(),
                response == null ? null : sanitizeHeaders(response.headers().map()),
                response == null ? null : sanitizeResponseBody(response.body())
        );
    }

    public static RequestTrace of(String url, Map<String, List<String>> headers) {
        return new RequestTrace(sanitizeUrl(URI.create(url)), sanitizeHeaders(headers), null, null, null, null);
    }

    public static RequestTrace of(
            String url,
            Map<String, List<String>> headers,
            String body,
            Integer responseStatusCode,
            Map<String, List<String>> responseHeaders,
            String responseBody
    ) {
        return new RequestTrace(
                sanitizeUrl(URI.create(url)),
                sanitizeHeaders(headers),
                body,
                responseStatusCode,
                sanitizeHeaders(responseHeaders),
                sanitizeResponseBody(responseBody)
        );
    }

    private static Map<String, List<String>> sanitizeHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (isAuthenticationHeader(name)) {
                continue;
            }
            sanitized.put(name, isSensitiveHeader(name) ? REDACTED_VALUE : List.copyOf(entry.getValue()));
        }
        return Map.copyOf(sanitized);
    }

    private static boolean isAuthenticationHeader(String name) {
        if (name == null) {
            return false;
        }
        return AUTHENTICATION_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private static boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        return SENSITIVE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private static String sanitizeUrl(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return uri.toString();
        }
        String sanitizedQuery = sanitizeQuery(query);
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getRawAuthority(),
                    uri.getRawPath(),
                    sanitizedQuery,
                    uri.getRawFragment()
            ).toString();
        } catch (Exception e) {
            return uri.getScheme() + "://" + uri.getRawAuthority() + uri.getRawPath()
                    + "?" + sanitizedQuery;
        }
    }

    private static String sanitizeQuery(String query) {
        String[] parts = query.split("&", -1);
        for (int i = 0; i < parts.length; i++) {
            int equals = parts[i].indexOf('=');
            String name = equals < 0 ? parts[i] : parts[i].substring(0, equals);
            if (isAuthenticationQueryParameter(name)) {
                parts[i] = name + "=<redacted>";
            }
        }
        return String.join("&", parts);
    }

    private static boolean isAuthenticationQueryParameter(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return "key".equals(normalized)
                || "api_key".equals(normalized)
                || "apikey".equals(normalized)
                || "access_token".equals(normalized)
                || "token".equals(normalized);
    }

    private static String sanitizeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return responseBody;
        }
        return SENSITIVE_JSON_STRING_FIELDS.matcher(responseBody).replaceAll("$1<redacted>$3");
    }
}
