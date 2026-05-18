package ch.thp.mas.llm.variance.analyze.semantic;

import java.util.Objects;

public record HierarchicalConfig(ScanRange threshold, HierarchicalLinkage linkage) {

    public HierarchicalConfig {
        Objects.requireNonNull(threshold, "threshold must not be null");
        Objects.requireNonNull(linkage, "linkage must not be null");
    }
}
