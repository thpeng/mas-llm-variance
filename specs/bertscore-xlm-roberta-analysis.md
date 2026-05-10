# BERTScore With XLM-RoBERTa

## Goal

Add a second semantic analysis path based on BERTScore with
`xlm-roberta-large`.

The motivation is to compare long answers on contextual token level instead of
compressing the whole answer into one sentence/document embedding. This should
make semantic distances more sensitive to concrete content changes such as
different cities, stops, entities, or sections inside otherwise similarly
structured answers.

This is introduced as an alternative algorithm, not as an immediate replacement
for the current E5 embedding pipeline. Operationally, both approaches should be
selectable so experiments can compare:

- E5 full-text embeddings
- E5 chunk-based distance
- BERTScore token-level distance

## Problem

The current semantic analysis primarily compares answer-level or chunk-level
embeddings with cosine distance. This is robust for broad semantic similarity,
but it can still hide important differences when answers share the same framing.

Example:

```text
Zuerich, Luzern, Interlaken, Bern, Genf, Basel
Zuerich, Luzern, Interlaken, Bern, Genf, Lausanne
```

A sentence or document embedding can rate these answers as nearly identical,
because most of the structure and content overlaps. For the research question,
however, the difference between `Basel` and `Lausanne` is relevant variance.

BERTScore compares contextual token embeddings and aggregates token-to-token
matches. This should preserve more detail while remaining language-independent
enough for German, French, Italian, and English.

## BERTScore Method

For two texts `candidate` and `reference`:

1. Tokenize both texts with `xlm-roberta-large`.
2. Run both token sequences through the model.
3. For every candidate token, find the maximum cosine similarity to any
   reference token. Aggregate this into BERTScore precision.
4. For every reference token, find the maximum cosine similarity to any
   candidate token. Aggregate this into BERTScore recall.
5. Compute BERTScore F1 from precision and recall.
6. Convert similarity to distance:

```text
distance = 1.0 - f1
```

The first implementation should use F1 as the clustering distance. Precision and
recall should still be retained in diagnostics where practical, because they can
help explain asymmetric differences.

## Architecture

Introduce a semantic distance abstraction so the analyzer can choose between
embedding-based and BERTScore-based distances.

Suggested model:

```text
semantic/
  SemanticDistanceService
  EmbeddingSemanticDistanceService
  BertScoreSemanticDistanceService
```

The existing E5 flow remains responsible for embedding texts and computing
cosine distances. The BERTScore flow is responsible for requesting pairwise
scores from the Python service and building the answer distance matrix directly.

The analyzer should then cluster using the selected distance matrix:

```text
answers
  -> semantic distance matrix
  -> selected clustering algorithm
  -> medoid/statistics
  -> syntactic analysis inside clusters
```

This keeps DBSCAN and hierarchical clustering reusable across both semantic
distance methods.

## Python Service

Extend or add a WSL-hosted Python API for BERTScore. It can live beside the
existing model service under:

```text
src/main/python
```

Expected endpoints:

```text
POST /load
POST /score
POST /unload
GET  /status
```

### `POST /load`

Loads `xlm-roberta-large` into GPU memory.

The model name should be configurable, defaulting to:

```text
xlm-roberta-large
```

### `POST /score`

Computes BERTScore for pairs of texts.

Request:

```json
{
  "pairs": [
    {
      "candidate": "Antwort A",
      "reference": "Antwort B"
    }
  ]
}
```

Response:

```json
{
  "model": "xlm-roberta-large",
  "count": 1,
  "scores": [
    {
      "precision": 0.91,
      "recall": 0.89,
      "f1": 0.90
    }
  ]
}
```

The Java side should not assume that precision and recall are symmetric. For
the pairwise distance matrix it should use `1 - f1`.

### `POST /unload`

Unloads the model and releases GPU memory.

The Java client must call unload in a `finally` block after a successful load
attempt, consistent with the E5 integration.

## Java Configuration

Add semantic distance configuration to `AnalysisConfig`.

Suggested enum:

```text
SemanticDistanceMethod
  EMBEDDING_COSINE
  BERTSCORE_F1
```

Defaults should preserve current behavior:

```text
EMBEDDING_COSINE
```

Environment variables:

```text
LLM_VARIANCE_SEMANTIC_DISTANCE_METHOD=embedding-cosine|bertscore-f1
LLM_VARIANCE_BERTSCORE_BASE_URL=http://localhost:8000
LLM_VARIANCE_BERTSCORE_MODEL=xlm-roberta-large
```

If E5 and BERTScore are served by different Python processes, the base URL must
allow separate ports. If they are served by the same process, the existing port
`8000` can be reused.

## Pairwise Scoring

For `n` answers, Java should create all unordered answer pairs `(i, j)` where
`i < j`.

The resulting matrix is:

```text
distance[i][i] = 0.0
distance[i][j] = 1.0 - f1(i, j)
distance[j][i] = distance[i][j]
```

The first implementation can send all pairs in one request. If payload size or
GPU memory becomes a problem, add batching behind the same client API.

## Clustering

Both clustering algorithms should remain available:

- `DBSCAN` over the precomputed BERTScore distance matrix.
- `HIERARCHICAL` over the precomputed BERTScore distance matrix.

Because BERTScore distances will have a different scale than E5 cosine
distances, thresholds must be configured separately in experiments. This is
acceptable because the selected method and threshold are written into the
analysis JSON.

No plan-specific evaluation logic should be added.

## Analysis Output

The analysis JSON should document:

- selected semantic distance method
- model name (`xlm-roberta-large`)
- BERTScore service base URL or logical service name
- clustering algorithm and threshold
- optional pairwise score diagnostics if they are not too large

The output should remain comparable by recording configuration, not by forcing
all algorithms onto the same threshold scale.

## Tests

Add unit tests for:

- BERTScore HTTP client lifecycle: load, score, unload.
- Unload is attempted when scoring fails after load.
- Score response validation:
  - count matches requested pairs
  - F1 is present and in `[0.0, 1.0]`
  - malformed response fails clearly
- Distance matrix construction:
  - diagonal is `0.0`
  - matrix is symmetric
  - distance is `1 - f1`
- Analyzer path with a fake BERTScore service/client.
- DBSCAN and hierarchical clustering can consume the BERTScore distance matrix.

Add integration test harness resources similar to the E5 harness:

```text
src/test/resources/analyze/integration/bertscore/
```

The harness should allow captured BERTScore responses to be stored as JSON so
tests can run without requiring the GPU service.

## Operational Experiment

Run the same captured long-answer datasets with:

1. E5 full-text + DBSCAN
2. E5 chunk-average-min + hierarchical complete linkage
3. BERTScore F1 distance + DBSCAN
4. BERTScore F1 distance + hierarchical complete linkage

The expected benefit is that BERTScore separates answers with different concrete
entities or itinerary details even when the overall wording and structure are
similar.

## Open Decisions

- Whether to host E5 and BERTScore in one Python service or two separate
  services/ports. -> same process. let the load command load the correct model.  
- Whether the Python implementation should use the `bert-score` package or a
  direct Hugging Face implementation. -> no opinion
- Whether pairwise scores should be persisted fully in the analysis JSON or only
  summarized to avoid large output files. -> fully persisted. 
- Initial operational thresholds for DBSCAN and hierarchical clustering with
  BERTScore distances. -> no opinion yet. 
