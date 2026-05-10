package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.semantic.BertScoreResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BertScoreHttpServiceTest {

    private HttpServer server;
    private final List<String> calls = new ArrayList<>();
    private final List<String> scoreBodies = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loadsScoresBuildsDistanceMatrixAndUnloads() throws Exception {
        startServer("""
                {"model":"xlm-roberta-large","count":3,"scores":[
                  {"precision":0.95,"recall":0.93,"f1":0.94},
                  {"precision":0.60,"recall":0.58,"f1":0.59},
                  {"precision":0.61,"recall":0.57,"f1":0.58}
                ]}
                """, 200);

        BertScoreResult result = service().score(List.of("A", "B", "C"), AnalysisConfig.defaults());

        assertThat(calls).containsExactly("/load", "/score", "/unload");
        assertThat(scoreBodies.getFirst()).contains("\"model\":\"xlm-roberta-large\"");
        assertThat(scoreBodies.getFirst()).contains("\"candidate\":\"A\"");
        assertThat(scoreBodies.getFirst()).contains("\"reference\":\"C\"");
        assertThat(result.distances()[0][0]).isEqualTo(0.0);
        assertThat(result.distances()[0][1]).isCloseTo(0.06, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(result.distances()[1][0]).isCloseTo(0.06, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(result.distances()[0][2]).isCloseTo(0.41, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(result.distances()[1][2]).isCloseTo(0.42, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(result.pairScores()).hasSize(3);
    }

    @Test
    void unloadsWhenScoreFailsAfterLoad() throws Exception {
        startServer("score failed", 500);

        assertThatThrownBy(() -> service().score(List.of("A", "B"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("BERTScore service /score failed");

        assertThat(calls).containsExactly("/load", "/score", "/unload");
    }

    @Test
    void rejectsMismatchedScoreCount() throws Exception {
        startServer("""
                {"model":"xlm-roberta-large","count":1,"scores":[]}
                """, 200);

        assertThatThrownBy(() -> service().score(List.of("A", "B"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("returned 0 scores");
    }

    @Test
    void rejectsInvalidF1() throws Exception {
        startServer("""
                {"model":"xlm-roberta-large","count":1,"scores":[
                  {"precision":0.9,"recall":0.9,"f1":1.2}
                ]}
                """, 200);

        assertThatThrownBy(() -> service().score(List.of("A", "B"), AnalysisConfig.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("f1 must be in [0.0, 1.0]");
    }

    @Test
    void rejectsIncompleteScore() throws Exception {
        startServer("""
                {"model":"xlm-roberta-large","count":1,"scores":[
                  {"precision":0.9,"recall":0.9}
                ]}
                """, 200);

        assertThatThrownBy(() -> service().score(List.of("A", "B"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incomplete score");
    }

    private void startServer(String scoreResponse, int scoreStatus) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/load", exchange -> respond(exchange, 200, "{\"status\":\"loaded\"}"));
        server.createContext("/score", exchange -> {
            scoreBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, scoreStatus, scoreResponse);
        });
        server.createContext("/unload", exchange -> respond(exchange, 200, "{\"status\":\"unloaded\"}"));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        calls.add(exchange.getRequestURI().getPath());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private BertScoreHttpService service() {
        return new BertScoreHttpService(
                "http://localhost:" + server.getAddress().getPort(),
                HttpClient.newHttpClient(),
                new ObjectMapper()
        );
    }
}
