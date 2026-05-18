# YAML Analysis Configuration and Plan Pairing

## Goal

The plan YAML should describe both the execution scenario and the analysis configuration that belongs to that scenario. A run still writes only the execution log. An analysis is invoked with a run log selection only. The application derives the required plan YAML from `runLog.planName` and must fail if that exact plan YAML is missing or lacks analysis configuration.

## YAML Shape

Keep the existing top-level run fields for compatibility with the current CLI and plan loader:

```yaml
inferenceProvider: LMSTUDIO
model: qwen/qwen3.5-9b
description: |
  Human-readable explanation of why this scenario and parameterization exists.

run:
  prompt: "..."
  iterations: 20
  temperature: 0.0
  topP: 1.0
  topK: 1
  seed: RANDOM
  reasoning: off
  load:
    contextLength: 4096
    evalBatchSize: 512
    flashAttention: true
    offloadKvCacheToGpu: true

analysis:
  embeddingProvider: e5-http
  embeddingBaseUrl: http://localhost:8000
  embeddingModel: intfloat/multilingual-e5-large
  embeddingPrefix: "passage:"
  maxEmbeddingTokens: 514
  semanticDistanceMethod: EMBEDDING_COSINE
  semanticRepresentation: CHUNK_AVERAGE_MIN
  chunk:
    targetTokens: 120
  distance: COSINE
  clusteringAlgorithm: HIERARCHICAL
  dbscan:
    epsilon: 0.15
    minPts: 2
  hierarchical:
    threshold: 0.08
    linkage: COMPLETE
  bleu:
    maxN: 4
    smoothingEpsilon: 0.1
  rouge:
    variant: ROUGE_L
    aggregation: F1
  percentile: NEAREST_RANK
```

All run sampling values must be supplied under `run`: `prompt`, `iterations`, `temperature`, `topP`, `topK`, `seed`, and `reasoning`. The seed can be a numeric value or `RANDOM`; `RANDOM` means the application does not send a seed and lets the model/provider choose. `reasoning` uses the central enum values `off`, `low`, `medium`, `high`, and `xhigh`; provider-specific mappings and unsupported combinations are handled by the client layer.

API connection details such as host URLs and authentication stay outside the plan YAML. LM Studio model load parameters may remain under `run.load` because they describe the model instance for the experiment, not the API transport.

All analysis configuration values can be supplied in the YAML. The `analysis` block itself and `analysis.clusteringAlgorithm` are required for analysis. Other omitted values may fall back to the established `AnalysisConfig.defaults()` so existing local tuning remains usable, but the sample plans should declare the full block to make intended thesis runs explicit.

## CLI Behavior

Run mode stays unchanged:

```bash
--plan=0001-rundreise-schweiz
--plans=0001-rundreise-schweiz,0002-hauptstadt-798
--plans=ALL
```

The repository may keep non-operative example plans under `src/test/resources/plans`; operative execution plans belong under `src/main/resources/plans`.

Analyze mode derives the plan from the run log:

```bash
--analyze=20260517-142141-263-0001-rundreise-schweiz.json
--analyze=ALL
```

Rules:

- Analysis must load the plan YAML named by `runLog.planName`.
- Analysis must fail if an explicit `--plan` or `--plans` is supplied in analyze mode.
- Analysis must fail if a run log has no matching plan YAML.
- Analysis must fail if the matching plan has no `analysis` block or no `clusteringAlgorithm`.

## Implementation

1. Add a JavaBean-style `YamlAnalysisConfig` to the `plan` package.
2. Add `analysis` to `YamlPlan`.
3. Add a mapper from `YamlAnalysisConfig` to `AnalysisConfig`, merging onto `AnalysisConfig.defaults()`.
4. Change `AnalyzeCommand` to load the exact plan YAML named by each run log's `planName`.
5. Pass the plan-derived `AnalysisConfig` into `Analyzer`.
6. Keep `RunLog` unchanged. It remains the raw execution record and does not persist analysis choices.

## Tests

- Plan loading reads the full nested `analysis` block.
- Analyze command loads the matching plan via `runLog.planName`.
- Analyze command rejects explicit plan selection in analyze mode.
- Analyze command rejects a plan without an `analysis` block.
- Analyzer uses the plan-derived clustering algorithm, e.g. DBSCAN or HIERARCHICAL, instead of environment defaults.
- Existing run mode tests continue to pass.
