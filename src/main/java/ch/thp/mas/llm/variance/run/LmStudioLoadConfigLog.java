package ch.thp.mas.llm.variance.run;

public record LmStudioLoadConfigLog(
        Integer contextLength,
        Integer evalBatchSize,
        Boolean flashAttention,
        Integer numExperts,
        Boolean offloadKvCacheToGpu
) {
}
