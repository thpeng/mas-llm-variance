# Literal Analysis Increment

## Goal

Add a literal analysis step to the analyzer.

The existing semantic and syntactic analyses intentionally normalize or abstract
away surface differences. Literal analysis adds a strict character-level
stability view over the raw model responses. It answers whether responses are
exactly identical, including punctuation, casing, whitespace, line breaks,
Markdown formatting, and every other character.

This is especially relevant for plans where stable output format matters, for
example machine-readable answers or downstream systems that rely on exact
patterns.

## Motivation

Semantic analysis can group answers that are meaningfully similar. Syntactic
analysis compares normalized token sequences with BLEU and ROUGE-L. Both are
useful for variance analysis, but neither captures exact output identity.

Examples of differences ignored or softened elsewhere:

- `Bern` vs `bern`
- `Bern.` vs `Bern`
- `Bern\nZuerich` vs `Bern Zuerich`
- Markdown list vs comma-separated list
- Different spacing or punctuation in otherwise identical content

Literal analysis does not judge correctness or quality. It only measures raw
output stability.

## Pipeline Placement

Extend the analyzer output from:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic
) {
}
```

to:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic,
        LiteralAnalysis literal
) {
}
```

The `literal` block is computed after semantic clusters are known because one
part of the literal analysis is calculated inside semantic clusters.

Recommended package:

```text
ch.thp.mas.llm.variance.analyze.literal
```

## Output Model

Add:

```java
public record LiteralAnalysis(
        boolean allResponsesIdentical,
        int responseCount,
        int distinctResponseCount,
        double exactMatchRate,
        List<LiteralClusterAnalysis> clusters
) {
}
```

Add:

```java
public record LiteralClusterAnalysis(
        int clusterId,
        int responseCount,
        int pairCount,
        int exactMatchPairCount,
        double exactMatchRate,
        int distinctResponseCount
) {
}
```

Field semantics:

- `allResponsesIdentical`: true if every raw response in the full run is
  exactly equal to every other response.
- `responseCount`: number of raw responses considered.
- `distinctResponseCount`: number of distinct raw response strings.
- `exactMatchRate`: pairwise exact match rate over the whole run.
- `clusters`: literal analysis inside each semantic cluster.
- `pairCount`: number of unordered response pairs in the scope.
- `exactMatchPairCount`: number of unordered response pairs whose raw strings
  are exactly equal.

## Metrics

### Full-Run Exact Identity

For a run with responses `r1..rN`:

```text
allResponsesIdentical = distinct(responses).size == 1
```

This follows the spirit of TARr@N: complete raw-answer agreement over repeated
runs.

Edge cases:

- `N == 0`: this should not occur because analyzer rejects empty response runs.
- `N == 1`: `allResponsesIdentical = true`, `pairCount = 0`,
  `exactMatchRate = 1.0`.

### Pairwise Exact Match Rate

For a scope with `N` responses:

```text
pairCount = N * (N - 1) / 2
exactMatchPairCount = count of pairs (i, j), i < j, where responses[i].equals(responses[j])
exactMatchRate = exactMatchPairCount / pairCount
```

If `pairCount == 0`, return `1.0`. A single response is trivially stable inside
its own scope.

### Cluster-Level Literal Stability

For each semantic cluster:

1. Take the repetition indices from `SemanticCluster.repetitionIndices()`.
2. Map them back to raw responses.
3. Compute `pairCount`, `exactMatchPairCount`, `exactMatchRate`, and
   `distinctResponseCount`.

Outliers are not semantic clusters and should not appear in the cluster list.
Their literal behavior is still reflected in the full-run metrics.

## Raw Text Rules

Literal analysis must compare the raw response strings exactly as stored in the
run log:

- no lowercasing
- no trimming
- no Unicode normalization
- no whitespace collapse
- no Markdown stripping
- no punctuation removal

Use `String.equals`.

If later Unicode normalization becomes relevant, it must be introduced as an
explicit configuration option and documented in the analysis JSON. It is not
part of this increment.

## Analyzer Integration

Add a `LiteralAnalyzer` component:

```text
ch.thp.mas.llm.variance.analyze.literal.LiteralAnalyzer
```

Suggested API:

```java
LiteralAnalysis analyze(List<String> responses, List<SemanticCluster> semanticClusters)
```

The main `Analyzer` should:

1. Read raw responses from the run log.
2. Compute semantic analysis.
3. Compute syntactic analysis using semantic clusters.
4. Compute literal analysis using the same raw responses and semantic clusters.
5. Return all three analysis blocks in `AnalysisResult`.

No model calls are needed for literal analysis.

## Configuration

No new runtime configuration is required for the first increment.

The method is intentionally fixed and strict. If the analysis JSON contains the
top-level `config`, no additional literal config is necessary.

## JSON Example

Example for three responses where two are exactly identical:

```json
{
  "literal": {
    "allResponsesIdentical": false,
    "responseCount": 3,
    "distinctResponseCount": 2,
    "exactMatchRate": 0.3333333333333333,
    "clusters": [
      {
        "clusterId": 0,
        "responseCount": 2,
        "pairCount": 1,
        "exactMatchPairCount": 1,
        "exactMatchRate": 1.0,
        "distinctResponseCount": 1
      },
      {
        "clusterId": 1,
        "responseCount": 1,
        "pairCount": 0,
        "exactMatchPairCount": 0,
        "exactMatchRate": 1.0,
        "distinctResponseCount": 1
      }
    ]
  }
}
```

## Tests

Add unit tests for `LiteralAnalyzer`:

- all responses identical
- all responses different
- some responses identical
- single response
- punctuation difference counts as different
- casing difference counts as different
- whitespace/newline difference counts as different
- cluster-level exact match rate
- semantic outliers are excluded from cluster literal list

Add analyzer integration tests:

- `AnalysisResult` contains `literal`.
- golden JSON fixture includes literal block.
- semantic clusters and literal clusters use the same cluster IDs.

Update existing golden fixtures:

```text
src/test/resources/analyze/expected/*.json
```

The stable single-word fixture should produce:

```text
allResponsesIdentical = true
distinctResponseCount = 1
exactMatchRate = 1.0
```

## Non-Goals

This increment does not:

- compute edit distance
- compute Levenshtein distance
- normalize Unicode
- compare JSON structurally
- validate output schemas
- judge factual correctness

Those can be added later as separate literal or structural analysis extensions.
