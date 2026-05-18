# Gemini API Integration

## Goal

Add Google Gemini as a first-class inference provider behind the existing `LlmClient` abstraction. Plans should be able to set `inferenceProvider: GEMINI`, use the existing `run` configuration block, and execute Gemini requests through the Gemini Generate Content API.

The integration must not put transport credentials or API host configuration into plan YAML. Authentication stays environment-driven.

## Sources

- Google Gemini Generate Content API: <https://ai.google.dev/api/generate-content>

## Current Architecture Fit

The existing provider boundary is:

- `InferenceProvider`
- `LlmClient`
- `LlmRequestConfig`
- `LlmResponse`
- `TokenUsage`

The Gemini implementation should add:

- `InferenceProvider.GEMINI`
- `GeminiClient implements LlmClient`
- unit tests for request mapping and response extraction

No changes should be required in `PlanRunner`, because it already calls the provider-selected `LlmClient` with the resolved `LlmRequestConfig`.

## Environment Configuration

Required:

- `GOOGLE_API_KEY`

Behavior:

- `InferenceProvider.GEMINI.createClient()` reads `GOOGLE_API_KEY`.
- If blank or absent, throw an actionable `IllegalStateException`.
- Do not add API key, endpoint, backend, or auth fields to YAML plans.

## Default Model

Set the first default to a current Gemini model that supports thinking configuration:

```java
return "gemini-3-flash";
```

The exact default may be changed later after local verification. Plans should normally set `model` explicitly for thesis runs.

## API Usage

The implementation uses the Generate Content REST endpoint directly:

```text
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={GOOGLE_API_KEY}
```

No Google SDK dependency is required for the current implementation. The Java SDK version originally considered exposed `thinkingBudget` rather than the requested `thinkingLevel` builder field, so REST is used to preserve the required `Reasoning -> thinkingLevel` mapping exactly.

Response text should be collected from all textual candidate parts under `candidates[].content.parts[].text`, preserving response order and joining multiple parts with newlines. If no non-blank text is present, fail with a clear exception. Do not silently accept an empty Gemini response as a valid repetition.

## Generation Config Mapping

Map existing `LlmRequestConfig` fields into Gemini `GenerationConfig`:

| `LlmRequestConfig` | Gemini REST `generationConfig` | Notes |
| --- | --- | --- |
| `temperature` | `temperature` | Optional, but currently required in plan `run`. |
| `topP` | `topP` | Optional at API level. Required in plan `run`. |
| `topK` | `topK` | Only set if non-null. Some Gemini models may reject `topK`; propagate the API error with context. |
| `seed` | `seed` | If plan has `seed: RANDOM`, resolver gives `null`; do not set seed. |
| `reasoning` | `thinkingConfig.thinkingLevel` | See mapping below. |

Do not map:

- API host
- API key
- project
- auth
- transport backend

## Thinking Level Mapping

Use the `Reasoning` enum as the plan-level abstraction. The YAML values are `off`, `low`, `medium`, `high`, and `xhigh`.
`on` is intentionally not accepted.

The enum documents all provider mappings explicitly:

| App `Reasoning` | LM Studio | Gemini | OpenAI | Anthropic |
| --- | --- | --- | --- | --- |
| `off` | `off` | `minimal` | `none` | `low` |
| `low` | `low` | `low` | `low` | `medium` |
| `medium` | `medium` | `medium` | `medium` | `high` |
| `high` | `high` | `high` | `high` | `xhigh` |
| `xhigh` | unsupported | unsupported | `xhigh` | `max` |

For Gemini, map the enum to thinking levels as follows:

| Plan `reasoning` | Gemini Thinking Level | Notes |
| --- | --- | --- |
| `off` | `minimal` | The internal app value `off` should mean the lowest Gemini thinking level. Gemini minimal is not guaranteed to disable thinking entirely. |
| `low` | `low` | Supported by Gemini 3.1 Pro, Gemini 3.1 Flash-Lite, Gemini 3 Flash. |
| `medium` | `medium` | Supported by Gemini 3.1 Pro, Gemini 3.1 Flash-Lite, Gemini 3 Flash. |
| `high` | `high` | Supported by Gemini 3.1 Pro, Gemini 3.1 Flash-Lite, Gemini 3 Flash. |
| `xhigh` | unsupported | Throw an exception before calling Gemini. `xhigh` is only valid for OpenAI and Anthropic. |

Model support matrix from the implementation requirement:

| Thinking Level | Gemini 3.1 Pro | Gemini 3.1 Flash-Lite | Gemini 3 Flash | Description |
| --- | --- | --- | --- | --- |
| `minimal` | Not supported | Supported (Default) | Supported | Lowest thinking level; minimizes latency; does not guarantee no thinking. |
| `low` | Supported | Supported | Supported | Lower latency and cost; simple instruction following and high-throughput use. |
| `medium` | Supported | Supported | Supported | Balanced reasoning for most tasks. |
| `high` | Supported (Default, Dynamic) | Supported (Dynamic) | Supported (Default, Dynamic) | Highest reasoning depth; may increase time to first output token. |

Validation:

- Parse `run.reasoning` through the central `Reasoning` enum.
- Reject `on` and `minimal` as plan values.
- In `GeminiClient`, reject `Reasoning.XHIGH` before sending the request.
- Do not add brittle model-name validation for `off -> minimal`. If a specific Gemini model rejects `minimal`, surface the provider error with model and reasoning context.

## Token Usage

Gemini responses expose usage metadata in `usageMetadata`. Map what is available into `TokenUsage`:

- input tokens -> prompt/input token count
- output tokens -> candidate/output token count
- total tokens -> total token count

If the response does not expose one of these values, set that field to `null` rather than inventing a value.

The run log must still store:

- full response text
- token usage when present
- model name from the plan
- provider `GEMINI`

Gemini does not need an explicit load/unload lifecycle.

## Plan Example

```yaml
inferenceProvider: GEMINI
model: gemini-3-flash
description: |
  Gemini run for the same scenario used across other providers.

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
  bleu:
    maxN: 4
    smoothingEpsilon: 0.1
  rouge:
    variant: ROUGE_L
    aggregation: F1
  percentile: NEAREST_RANK

run:
  prompt: "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an"
  iterations: 20
  temperature: 0.0
  topP: 1.0
  topK: 1
  seed: RANDOM
  reasoning: low
```

## Implementation Steps

1. Add `GeminiClient` using REST Generate Content.

2. Add `GEMINI` enum value to `InferenceProvider`.

3. Use the central `Reasoning` enum and its `geminiThinkingLevel()` mapping.

4. Map Gemini thinking levels inside `GeminiClient`.

5. Map generation parameters from `LlmRequestConfig` into REST `generationConfig`.

6. Map response text and token usage into `LlmResponse`.

7. Add tests.

## Tests

### Unit Tests

`InferenceProviderTest`

- `GEMINI` rejects missing `GOOGLE_API_KEY`.
- `GEMINI.defaultModel()` returns the chosen default.

`PlanResolverTest`

- accepts `reasoning=off|low|medium|high|xhigh`.
- rejects `reasoning=on` and `reasoning=minimal`.
- `seed: RANDOM` still resolves to `null`.

`GeminiClientTest`

- maps `temperature`, `topP`, `topK`, numeric `seed`, and `reasoning=low`.
- omits `seed` when `seed` is null.
- maps `reasoning=off` to Gemini `minimal`.
- rejects `reasoning=xhigh` before the request.
- propagates provider errors with model/reasoning context when Gemini rejects a thinking level.
- extracts `candidates[].content.parts[].text` into `LlmResponse.text`.
- maps usage metadata when available.
- fails on blank response text.

Use a local fake HTTP server for unit tests. Do not call the real Google API in unit tests.

### Optional Manual Smoke Test

With `GOOGLE_API_KEY` set:

```bash
./gradlew bootRun --args="--plan=<gemini-plan-name> --iterations=1"
```

Expected:

- run log is written under `src/main/resources/runs`
- `inferenceProvider` is `GEMINI`
- response text is present
- token usage is present if exposed by the API response

## Open Implementation Notes

- The Java SDK `com.google.genai:google-genai:1.0.0` exposes `ThinkingConfig.thinkingBudget`, not `thinkingLevel`. Keep REST and no Google SDK dependency until the project chooses a newer SDK version or a different Gemini reasoning abstraction.
- If `topK` is rejected by a specific Gemini model, keep surfacing the provider error. Do not silently drop `topK`, because thesis runs need the requested parameterization to be explicit.
