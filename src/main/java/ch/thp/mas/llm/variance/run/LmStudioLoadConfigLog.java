package ch.thp.mas.llm.variance.run;

public record LmStudioLoadConfigLog(
        Integer contextLength,
        Integer evalBatchSize,
        Boolean flashAttention,
        Integer numExperts,
        Boolean offloadKvCacheToGpu,
        Long seed
) {

    public LmStudioLoadConfigLog(
            Integer contextLength,
            Integer evalBatchSize,
            Boolean flashAttention,
            Integer numExperts,
            Boolean offloadKvCacheToGpu
    ) {
        this(contextLength, evalBatchSize, flashAttention, numExperts, offloadKvCacheToGpu, null);
    }
}
