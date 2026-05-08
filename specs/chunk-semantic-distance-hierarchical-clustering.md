# Chunk Semantic Distance And Hierarchical Clustering

## Goal

Add an alternative semantic analysis mode for long generated answers where one
full-document embedding is too coarse. The mode must remain use-case agnostic so
results across different plans stay comparable.

## Problem

For long answers, full-document embeddings can be dominated by shared framing,
introductory text, headings, and repeated task wording. Two answers with
materially different details can still receive a very small cosine distance.
DBSCAN can then merge them into one cluster, especially when answers form a
chain of near-neighbor relations.

## Approach

Introduce two independently configurable dimensions:

1. Semantic representation:
   - `FULL_TEXT`: current behavior, one embedding per answer.
   - `CHUNK_AVERAGE_MIN`: split each answer into generic text chunks, embed all
     chunks, and compute answer-to-answer distance as symmetric average minimum
     chunk distance.
2. Clustering algorithm:
   - `DBSCAN`: current behavior.
   - `HIERARCHICAL`: agglomerative clustering over the precomputed answer
     distance matrix.

This keeps the evaluation generic. No plan-specific extraction, no route parser,
and no domain-specific target schema is introduced.

## Chunking

The first implementation uses a deterministic generic chunker:

- Split answers on blank lines.
- Merge small neighboring blocks until a target token count is reached.
- Keep chunks below the configured maximum embedding token count.
- Preserve input order.
- Never produce zero chunks for a non-empty answer.

The chunker is intentionally simple and reproducible. More advanced chunkers can
be added later behind the same representation setting.

## Chunk-Based Distance

For two answers A and B:

1. Embed all chunks from A and B.
2. Compute pairwise cosine distances between chunks.
3. For every chunk in A, find the closest chunk in B and average those distances.
4. For every chunk in B, find the closest chunk in A and average those distances.
5. The answer distance is the mean of both directed averages.

This is a symmetric Chamfer-style distance. It rewards shared content while still
penalizing added or changed sections.

## Hierarchical Clustering

Add agglomerative clustering over the answer distance matrix:

- Start with one cluster per answer.
- Repeatedly merge the closest pair of clusters.
- Stop when the closest cluster-pair distance is above the configured threshold.
- Support `COMPLETE` and `AVERAGE` linkage.
- No noise/outlier label is produced by this algorithm.

`COMPLETE` is useful when chaining is a problem because all members of the merged
cluster must remain within the threshold. `AVERAGE` is less strict and can be
used in exploratory tests.

## Configuration

Add fields to `AnalysisConfig`:

- `semanticRepresentation`
- `chunk`
- `clusteringAlgorithm`
- `hierarchical`

Environment variables:

- `LLM_VARIANCE_SEMANTIC_REPRESENTATION=full-text|chunk-average-min`
- `LLM_VARIANCE_CLUSTERING_ALGORITHM=dbscan|hierarchical`
- `LLM_VARIANCE_CHUNK_TARGET_TOKENS=120`
- `LLM_VARIANCE_HIERARCHICAL_THRESHOLD=0.08`
- `LLM_VARIANCE_HIERARCHICAL_LINKAGE=complete|average`

Defaults preserve current behavior:

- `FULL_TEXT`
- `DBSCAN`

## Tests

Add tests for:

- Paragraph chunking and max-token splitting.
- Chunk average-min distance, including extra-section penalty.
- Hierarchical clustering with complete linkage preventing DBSCAN-style chains.
- Analyzer path for `CHUNK_AVERAGE_MIN + HIERARCHICAL` using fake embeddings.
- Existing DBSCAN/full-text tests continue to pass unchanged.

## Operational Use

Early experiments can run the same captured embeddings/tests with several
configurations:

- Full text + DBSCAN
- Full text + hierarchical complete linkage
- Chunk average-min + hierarchical complete linkage
- Chunk average-min + hierarchical average linkage

The selected configuration is written into the analysis JSON through
`AnalysisConfig`, so different analysis runs remain auditable.
