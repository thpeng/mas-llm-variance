package ch.thp.mas.llm.variance.plan;

public class LmStudioLoadConfig {

    private Integer contextLength;
    private Integer evalBatchSize;
    private Boolean flashAttention;
    private Integer numExperts;
    private Boolean offloadKvCacheToGpu;

    public Integer getContextLength() {
        return contextLength;
    }

    public void setContextLength(Integer contextLength) {
        this.contextLength = contextLength;
    }

    public Integer getEvalBatchSize() {
        return evalBatchSize;
    }

    public void setEvalBatchSize(Integer evalBatchSize) {
        this.evalBatchSize = evalBatchSize;
    }

    public Boolean getFlashAttention() {
        return flashAttention;
    }

    public void setFlashAttention(Boolean flashAttention) {
        this.flashAttention = flashAttention;
    }

    public Integer getNumExperts() {
        return numExperts;
    }

    public void setNumExperts(Integer numExperts) {
        this.numExperts = numExperts;
    }

    public Boolean getOffloadKvCacheToGpu() {
        return offloadKvCacheToGpu;
    }

    public void setOffloadKvCacheToGpu(Boolean offloadKvCacheToGpu) {
        this.offloadKvCacheToGpu = offloadKvCacheToGpu;
    }
}
