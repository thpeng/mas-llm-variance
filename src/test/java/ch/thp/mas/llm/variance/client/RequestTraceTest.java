package ch.thp.mas.llm.variance.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestTraceTest {

    @Test
    void dropsAuthenticationHeadersAndRedactsSensitiveTraceHeaders() {
        RequestTrace trace = RequestTrace.of(
                "https://api.openai.com/v1/responses",
                Map.of(
                        "Authorization", List.of("Bearer secret"),
                        "Content-Type", List.of("application/json"),
                        "Set-Cookie", List.of("__cf_bm=secret-cookie"),
                        "openai-organization", List.of("org-secret"),
                        "openai-project", List.of("proj-secret"),
                        "anthropic-organization-id", List.of("anthropic-org-secret"),
                        "x-request-id", List.of("req-secret"),
                        "cf-ray", List.of("trace-secret"),
                        "traceresponse", List.of("trace-response-secret")
                ),
                "{}",
                200,
                Map.of(
                        "Content-Type", List.of("application/json"),
                        "set-cookie", List.of("__cf_bm=response-cookie"),
                        "openai-organization", List.of("org-response"),
                        "openai-project", List.of("proj-response"),
                        "anthropic-organization-id", List.of("anthropic-org-response"),
                        "x-request-id", List.of("req-response"),
                        "cf-ray", List.of("trace-response"),
                        "traceresponse", List.of("trace-response-value")
                ),
                "{}"
        );

        assertThat(trace.headers()).doesNotContainKey("Authorization");
        assertThat(trace.headers()).containsEntry("Content-Type", List.of("application/json"));
        assertThat(trace.headers()).containsEntry("Set-Cookie", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("openai-organization", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("openai-project", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("anthropic-organization-id", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("x-request-id", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("cf-ray", List.of("<redacted>"));
        assertThat(trace.headers()).containsEntry("traceresponse", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("Content-Type", List.of("application/json"));
        assertThat(trace.responseHeaders()).containsEntry("set-cookie", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("openai-organization", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("openai-project", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("anthropic-organization-id", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("x-request-id", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("cf-ray", List.of("<redacted>"));
        assertThat(trace.responseHeaders()).containsEntry("traceresponse", List.of("<redacted>"));
    }

    @Test
    void redactsAuthenticationQueryParameters() {
        RequestTrace trace = RequestTrace.of(
                "https://generativelanguage.googleapis.com/v1beta/models/model:generateContent?key=secret&other=value",
                Map.of()
        );

        assertThat(trace.url()).contains("key=%3Credacted%3E");
        assertThat(trace.url()).contains("other=value");
        assertThat(trace.url()).doesNotContain("secret");
    }

    @Test
    void redactsSensitiveResponseBodyFields() {
        RequestTrace trace = RequestTrace.of(
                "https://api.example.test/v1/messages",
                Map.of(),
                "{}",
                200,
                Map.of(),
                """
                        {
                          "id": "msg_secret",
                          "responseId": "resp_secret",
                          "content": [
                            {
                              "type": "thinking",
                              "signature": "anthropic_signature_secret",
                              "thoughtSignature": "gemini_thought_signature_secret"
                            },
                            {
                              "type": "text",
                              "text": "visible answer"
                            }
                          ]
                        }
                        """
        );

        assertThat(trace.responseBody()).contains("\"id\": \"<redacted>\"");
        assertThat(trace.responseBody()).contains("\"responseId\": \"<redacted>\"");
        assertThat(trace.responseBody()).contains("\"signature\": \"<redacted>\"");
        assertThat(trace.responseBody()).contains("\"thoughtSignature\": \"<redacted>\"");
        assertThat(trace.responseBody()).contains("visible answer");
        assertThat(trace.responseBody()).doesNotContain("msg_secret");
        assertThat(trace.responseBody()).doesNotContain("resp_secret");
        assertThat(trace.responseBody()).doesNotContain("anthropic_signature_secret");
        assertThat(trace.responseBody()).doesNotContain("gemini_thought_signature_secret");
    }
}
