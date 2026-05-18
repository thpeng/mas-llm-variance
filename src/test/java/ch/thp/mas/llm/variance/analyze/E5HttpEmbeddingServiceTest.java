package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.analyze.semantic.ChunkConfig;
import ch.thp.mas.llm.variance.analyze.semantic.ClusteringAlgorithm;
import ch.thp.mas.llm.variance.analyze.semantic.DbscanConfig;
import ch.thp.mas.llm.variance.analyze.semantic.DistanceMetric;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalConfig;
import ch.thp.mas.llm.variance.analyze.semantic.HierarchicalLinkage;
import ch.thp.mas.llm.variance.analyze.semantic.ScanRange;
import ch.thp.mas.llm.variance.analyze.semantic.SemanticRepresentation;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
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

class E5HttpEmbeddingServiceTest {

    private HttpServer server;
    private final List<String> calls = new ArrayList<>();
    private final List<String> embedBodies = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callsLoadEmbedUnloadAndReturnsEmbeddings() throws Exception {
        startServer(200, 200, 200, """
                {"dim":2,"count":2,"embeddings":[[1.0,0.0],[0.0,1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        List<EmbeddingResult> embeddings = service.embed(List.of("Antwort 1", "Antwort 2"), AnalysisConfig.defaults());

        assertThat(calls).containsExactly("/load", "/embed", "/unload");
        assertThat(embedBodies.getFirst()).contains("passage: Antwort 1", "passage: Antwort 2");
        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.getFirst().vector()).containsExactly(1.0, 0.0);
        assertThat(embeddings.getFirst().truncated()).isFalse();
    }

    @Test
    void blankPrefixDoesNotAddLeadingWhitespace() throws Exception {
        startServer(200, 200, 200, """
                {"dim":1,"count":1,"embeddings":[[1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        service.embed(List.of("Antwort"), configWithBlankPrefix());

        assertThat(embedBodies.getFirst()).contains("\"Antwort\"");
        assertThat(embedBodies.getFirst()).doesNotContain(" passage");
    }

    @Test
    void unloadsWhenEmbedFails() throws Exception {
        startServer(200, 500, 200, "boom");
        E5HttpEmbeddingService service = service();

        assertThatThrownBy(() -> service.embed(List.of("Antwort"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("/embed failed");
        assertThat(calls).containsExactly("/load", "/embed", "/unload");
    }

    @Test
    void doesNotEmbedWhenLoadFails() throws Exception {
        startServer(500, 200, 200, """
                {"dim":1,"count":1,"embeddings":[[1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        assertThatThrownBy(() -> service.embed(List.of("Antwort"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("/load failed");
        assertThat(calls).containsExactly("/load");
    }

    @Test
    void rejectsCountMismatch() throws Exception {
        startServer(200, 200, 200, """
                {"dim":1,"count":2,"embeddings":[[1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        assertThatThrownBy(() -> service.embed(List.of("Antwort"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("returned count");
        assertThat(calls).containsExactly("/load", "/embed", "/unload");
    }

    @Test
    void rejectsVectorDimensionMismatch() throws Exception {
        startServer(200, 200, 200, """
                {"dim":2,"count":1,"embeddings":[[1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        assertThatThrownBy(() -> service.embed(List.of("Antwort"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("does not match response dim");
    }

    @Test
    void failsWhenUnloadFailsAfterSuccessfulLoad() throws Exception {
        startServer(200, 200, 500, """
                {"dim":1,"count":1,"embeddings":[[1.0]]}
                """);
        E5HttpEmbeddingService service = service();

        assertThatThrownBy(() -> service.embed(List.of("Antwort"), AnalysisConfig.defaults()))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("/unload failed");
    }

    private void startServer(int loadStatus, int embedStatus, int unloadStatus, String embedBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/load", exchange -> respond(exchange, loadStatus, "{\"status\":\"loaded\"}"));
        server.createContext("/embed", exchange -> {
            embedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, embedStatus, embedBody);
        });
        server.createContext("/unload", exchange -> respond(exchange, unloadStatus, "{\"status\":\"unloaded\"}"));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        calls.add(exchange.getRequestURI().getPath());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private E5HttpEmbeddingService service() {
        return new E5HttpEmbeddingService(
                "http://localhost:" + server.getAddress().getPort(),
                HttpClient.newHttpClient(),
                new ObjectMapper()
        );
    }

    private AnalysisConfig configWithBlankPrefix() {
        return new AnalysisConfig(
                "e5-http",
                "http://localhost:" + server.getAddress().getPort(),
                "intfloat/multilingual-e5-large",
                "",
                514,
                AnalysisConfig.defaults().semanticDistanceMethod(),
                SemanticRepresentation.FULL_TEXT,
                new ChunkConfig(120),
                DistanceMetric.COSINE,
                ClusteringAlgorithm.DBSCAN,
                0.01,
                new DbscanConfig(ScanRange.ofHundredths(15, 15), 2),
                new HierarchicalConfig(ScanRange.ofHundredths(8, 8), HierarchicalLinkage.COMPLETE),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1),
                PercentileMethod.NEAREST_RANK
        );
    }
}
