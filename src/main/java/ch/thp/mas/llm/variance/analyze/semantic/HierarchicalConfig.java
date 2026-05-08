package ch.thp.mas.llm.variance.analyze.semantic;

import java.util.Objects;

public record HierarchicalConfig(double threshold, HierarchicalLinkage linkage) {

    public HierarchicalConfig {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be non-negative");
        }
        Objects.requireNonNull(linkage, "linkage must not be null");
    }
}
