# LLM Preprocess Clustering

## Goal

Add an analysis representation mode where a local LLM first extracts task-specific information from each tested-LLM answer. The extracted string is then embedded and clustered with the existing DBSCAN or hierarchical scan pipeline.

Example prompt:

```text
Extract the destinations of the following text and emit them comma-separated.
```

Example extracted representation:

```text
Zuerich, Luzern, Interlaken, Zermatt, Montreux
```

The local LLM is the Nemotron variant available through LM Studio. It is an analysis helper, not the tested model.

## Motivation

Long answers can share wording and structure while differing in the information that matters for a scenario. For example, two travel plans can look semantically similar overall while containing different destinations. Full-text embeddings may hide that difference.

The preprocessing path narrows the comparison to the scenario-relevant information:

1. The tested LLM produces raw answers.
2. The local Nemotron model extracts the configured information.
3. The extracted strings are embedded.
4. Existing DBSCAN or hierarchical scan clustering groups the answers.

## Scope and Non-Goals

This increment adds a new semantic representation mode. It does not replace full-text or chunk-based analysis.

The extraction result is treated as analysis input, not as a correctness oracle. Extraction quality depends on the local model and prompt and must be visible in the output.

The first increment accepts plain text extraction output. JSON/schema-constrained extraction is left for a later increment.

## YAML Configuration

Add a preprocess block under `analysis` and select it through the semantic representation:

```yaml
analysis:
  semanticDistanceMethod: EMBEDDING_COSINE
  semanticRepresentation: LLM_PREPROCESS
  preprocess:
    provider: LMSTUDIO
    model: nvidia/nemotron-3-nano-4b
    prompt: |
      Extract the destinations of the following travel plan.
      Emit only the destinations as a comma-separated list.
      Do not add explanations.
    temperature: 0.0
    topP: 1.0
    topK: 1
    seed: RANDOM
    reasoning: off
  clusteringAlgorithm: HIERARCHICAL
  scanIncrement: 0.01
  dbscan:
    epsilon:
      from: 0.05
      to: 0.15
    minPts: 2
  hierarchical:
    threshold:
      from: 0.03
      to: 0.12
    linkage: COMPLETE
```

Rules:

- `analysis.preprocess` is required when `semanticRepresentation: LLM_PREPROCESS`.
- `analysis.preprocess.prompt` is required and may be multiline.
- `provider` is initially restricted to `LMSTUDIO`.
- `model` is required.
- Sampling parameters reuse the existing value types: `temperature`, `topP`, `topK`, `seed`, and `reasoning`.
- API transport details such as host, token, and timeout remain outside YAML.

## Prompt Construction

For each raw answer, the application calls the local model with:

```text
<configured preprocess prompt>

Text:
<raw tested-LLM answer>
```

The response is trimmed and used as the semantic input text for embedding. Empty extraction responses are recorded; if all extracted texts are empty, analysis aborts with a clear error.

## Algorithm Flow

1. Read the run log and matching plan YAML.
2. Extract raw tested-LLM responses.
3. If `semanticRepresentation` is `LLM_PREPROCESS`, open the configured local LM Studio inference session.
4. For each raw response, call the local model with the configured prompt and raw answer.
5. Store the extracted text by repetition index.
6. Close or unload the local model if this analysis loaded it.
7. Embed the extracted texts.
8. Compute pairwise cosine distances.
9. Run the configured DBSCAN or hierarchical scan.
10. Compute syntactic metrics on the original raw answers inside each semantic cluster.
11. Compute literal analysis once on the original raw answers.
12. Write one analysis JSON.

Important distinction:

- Semantic clustering uses extracted texts.
- Syntactic analysis uses original raw answers.
- Literal analysis uses original raw answers.

## Output Model

Make preprocessing auditable in the analysis JSON:

```java
public record PreprocessAnalysis(
        String provider,
        String model,
        String prompt,
        List<PreprocessedResponse> responses
) {
}
```

```java
public record PreprocessedResponse(
        int repetitionIndex,
        String sourceResponse,
        String extractedText
) {
}
```

Extend the result:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        PreprocessAnalysis preprocess,
        List<AnalysisScan> scans,
        LiteralAnalysis literal
) {
}
```

`preprocess` is `null` when no LLM preprocessing is used.

## Cluster Scan Behavior

No new clustering algorithm is required. `LLM_PREPROCESS` changes only the representation before embedding.

The existing scan semantics remain:

- `DBSCAN` scans `dbscan.epsilon`.
- `HIERARCHICAL` scans `hierarchical.threshold`.
- Each scan reports `clusterCount`, semantic clusters, syntactic clusters, and outliers.

## Client and Lifecycle

Use the existing LM Studio infrastructure.

Requirements:

- Load the preprocess model before extraction when lifecycle control is enabled.
- Unload it after extraction if it was loaded by the analysis.
- Do not assume that the tested model is still loaded.
- Use LM Studio's native chat endpoint.
- Capture preprocess token usage if available, but keep it separate from tested-model run metrics.

Failure behavior:

- If a preprocess call fails, abort analysis and write no partial analysis file.
- If the local model returns no message output, fail with repetition index and model name.
- If all extracted texts are empty, abort because clustering would be meaningless.

## Configuration Model

Suggested config record:

```java
public record PreprocessConfig(
        InferenceProvider provider,
        String model,
        String prompt,
        Double temperature,
        Double topP,
        Integer topK,
        SeedValue seed,
        Reasoning reasoning
) {
}
```

Add `PreprocessConfig preprocess` to `AnalysisConfig`.

Validation:

- Required when `semanticRepresentation == LLM_PREPROCESS`.
- Initially only `InferenceProvider.LMSTUDIO` is supported.
- `prompt` and `model` must not be blank.

## Testing Plan

Mapper tests:

- Maps `semanticRepresentation: LLM_PREPROCESS`.
- Maps multiline `analysis.preprocess.prompt`.
- Rejects missing `analysis.preprocess`.
- Rejects missing prompt.
- Rejects non-LM Studio provider.

Preprocess service tests:

- Fake local LLM returns extracted comma-separated strings.
- Requests contain the configured prompt and raw answer.
- Extraction order is preserved by repetition index.
- All-empty extraction aborts analysis.
- Failed extraction writes no output.

Analyzer tests:

- Different raw answers that extract to the same destination list cluster together.
- Different extracted destination lists separate under hierarchical scan.
- `PreprocessAnalysis` contains one entry per response.
- Semantic clustering uses extracted texts.
- Syntactic and literal analysis still use raw responses.
- Scan entries still include `clusterCount`.

Integration harness:

- Optional/skipped unless LM Studio is reachable.
- Uses the local Nemotron model.
- Uses a travel-plan fixture.
- Prints scan value, cluster count, and extracted texts.

## Open Questions

- Should a later increment support JSON extraction with schema validation?
- Should comma-separated outputs be normalized, for example by trimming, sorting, or deduplicating values?
- Should preprocessing results be cached by hash of prompt, model, and source response?
- Should the preprocess model load response be written into the analysis file?
