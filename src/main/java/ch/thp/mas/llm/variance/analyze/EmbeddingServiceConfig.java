package ch.thp.mas.llm.variance.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingServiceConfig {

    @Bean
    public EmbeddingService embeddingService(TextTokenizer tokenizer) {
        AnalysisConfig config = AnalysisConfig.defaults();
        return switch (config.embeddingProvider()) {
            case "e5-http" -> new E5HttpEmbeddingService(
                    config.embeddingBaseUrl(),
                    HttpClient.newHttpClient(),
                    new ObjectMapper()
            );
            case "local-hashing" -> new LocalHashingEmbeddingService(tokenizer);
            default -> throw new AnalysisException("Unknown embedding provider: " + config.embeddingProvider());
        };
    }

}
