package ch.thp.mas.llm.variance.analyze.semantic;

public enum SemanticDistanceMethod {
    EMBEDDING_COSINE,

    /**
     * Deprecated experimental path. Full-answer BERTScore with XLM-RoBERTa did
     * not improve cluster differentiation for the long-answer fixtures because
     * shared answer structure and truncation dominated the token-level score.
     *
     * see 3.4.3 in the thesis
     */
    @Deprecated(forRemoval = false)
    BERTSCORE_F1
}
