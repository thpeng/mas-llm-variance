package ch.thp.mas.llm.variance.analyze;

public record ChunkConfig(int targetTokens) {

    public ChunkConfig {
        if (targetTokens < 1) {
            throw new IllegalArgumentException("targetTokens must be at least 1");
        }
    }
}
