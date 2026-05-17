# LM Studio Run Lifecycle and Inference Provider Rename

## Goal

Rewrite the plan/run mechanism so LM Studio executions explicitly manage the model lifecycle:

1. Check whether the LM Studio control server is reachable.
2. Load the configured model before the first repetition.
3. Execute all repetitions against the loaded model.
4. Unload the loaded model instance after the final repetition, also when a repetition fails after loading.

At the same time, rename the domain concept `Manufacturer` to `InferenceProvider`. The current name describes a vendor, but the application actually selects an inference runtime/provider such as OpenAI, Anthropic, or a local LM Studio instance.

No execution or analysis behavior should be changed for OpenAI and Anthropic beyond the naming refactor. LM Studio should stop using the Anthropic-compatible endpoint and use LM Studio's native REST chat endpoint instead.

## User-Facing Configuration

Plan files should use `inferenceProvider` instead of `manufacturer`:

```yaml
inferenceProvider: LMSTUDIO
model: mistralai/ministral-3-8b
prompt: "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an"
iterations: 50
temperature: 0.0
seed: 1
topP: 1
topK: 1
reasoning: off
```

The old `manufacturer` field is removed. The software is still in verification and not operational, so no backward compatibility layer is required.

Optional LM Studio load parameters may be added to a plan under a dedicated `load` object:

```yaml
inferenceProvider: LMSTUDIO
model: mistralai/ministral-3-8b
load:
  contextLength: 8192
  evalBatchSize: 512
  flashAttention: true
  numExperts: null
  offloadKvCacheToGpu: true
prompt: "..."
iterations: 50
reasoning: off
```

Only LM Studio uses the `load` object. OpenAI and Anthropic ignore it, or validation may reject it for non-LM Studio providers if a stricter configuration style is preferred.

`reasoning` controls LM Studio's native chat reasoning mode. It is optional and defaults to `off`.

Allowed values:

```text
off
low
medium
high
on
```

If the selected model does not support the requested reasoning setting, LM Studio will fail the chat request. The application should surface that failure clearly and still unload the model if this run loaded it.

## Environment

LM Studio model lifecycle and invocation both use the native LM Studio developer REST API.

Suggested environment variables:

- `LMSTUDIO_BASE_URL`: LM Studio native API endpoint. Default `http://localhost:1234`.
- `LM_API_TOKEN`: optional bearer token. If present, send `Authorization: Bearer <token>` on LM Studio requests.

The old Anthropic-compatible LM Studio generation endpoint is removed from the plan/run path. LM Studio calls should use `/api/v1/models`, `/api/v1/models/load`, `/api/v1/chat`, and `/api/v1/models/unload` on the same configured native base URL.

## LM Studio Control API

### Server Check

Before loading, call:

```http
GET /api/v1/models
```

Expected response:

```json
{
  "models": [
    {
      "type": "llm",
      "key": "mistralai/ministral-3-8b",
      "display_name": "Ministral 3 8B",
      "loaded_instances": []
    }
  ]
}
```

Behavior:

- If the endpoint is unreachable, fail the run before any repetition starts.
- If the configured model key is absent, fail the run with a clear message listing the requested model.
- If the model is already loaded, decide by model key:
  - Prefer reusing the existing loaded instance if it matches the requested model.
  - Still record the instance id so the lifecycle can be reasoned about.
  - Do not unload a model instance that was already loaded before this run unless the implementation explicitly loaded it for this run.

The last point avoids accidentally unloading a manually prepared LM Studio model that the user still needs.

### Load

Before repetitions, call:

```http
POST /api/v1/models/load
Content-Type: application/json
```

Request body:

```json
{
  "model": "mistralai/ministral-3-8b",
  "context_length": 8192,
  "eval_batch_size": 512,
  "flash_attention": true,
  "num_experts": null,
  "offload_kv_cache_to_gpu": true,
  "echo_load_config": true
}
```

Mapping from YAML to API fields:

| Plan field | LM Studio field |
| --- | --- |
| `model` | `model` |
| `load.contextLength` | `context_length` |
| `load.evalBatchSize` | `eval_batch_size` |
| `load.flashAttention` | `flash_attention` |
| `load.numExperts` | `num_experts` |
| `load.offloadKvCacheToGpu` | `offload_kv_cache_to_gpu` |
| always `true` | `echo_load_config` |

Omit null optional fields from the JSON request.

The unload endpoint needs an `instance_id`. The implementation should resolve it robustly:

1. Use an instance id returned by `/load` if LM Studio provides one.
2. Otherwise call `GET /api/v1/models` after load and find the loaded instance for the configured model key.
3. If no loaded instance can be found, fail the run before repetitions start.

### Chat Invocation

After load, execute the existing repetition loop:

```text
for i in 1..iterations:
  call LlmClient.call(prompt, config)
  record startedAt, endedAt, full response, token usage
```

For LM Studio, `LlmClient.call(...)` must use the native chat endpoint:

```http
POST /api/v1/chat
Content-Type: application/json
```

Request body:

```json
{
  "model": "mistralai/ministral-3-8b",
  "input": "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an",
  "temperature": 0.0,
  "top_p": 1.0,
  "top_k": 1,
  "reasoning": "off",
  "store": false
}
```

Mapping from plan/request config to LM Studio chat fields:

| Plan/request field | LM Studio chat field |
| --- | --- |
| `model` | `model` |
| `prompt` | `input` |
| `temperature` | `temperature` |
| `topP` | `top_p` |
| `topK` | `top_k` |
| `reasoning` | `reasoning` |
| optional future `maxOutputTokens` | `max_output_tokens` |
| optional future `contextLength` | `context_length` |
| fixed `false` | `store` |

Use `store: false` by default because every repetition is meant to be independent and auditable through the application's own run log. Do not send `previous_response_id`.

The response contains an `output` array. The application should build the model response text from output items with:

```json
{
  "type": "message",
  "content": "..."
}
```

Rules:

- Concatenate all `message.content` values in response order, separated by a newline if more than one message is present.
- Ignore `reasoning` items for the answer text.
- Ignore `tool_call` and `invalid_tool_call` items for the answer text because plans currently do not enable integrations.
- If no `message` item exists, fail the repetition with a clear error.

Token usage maps from `stats`:

| LM Studio stats field | Internal token field |
| --- | --- |
| `input_tokens` | `inputTokens` |
| `total_output_tokens` | `outputTokens` |
| `input_tokens + total_output_tokens` | `totalTokens` |

`reasoning_output_tokens`, `tokens_per_second`, `time_to_first_token_seconds`, `model_load_time_seconds`, and `response_id` are LM Studio-specific metadata. They may be logged later as provider metadata, but they are not required for the MAS token usage fields.

The response also contains `model_instance_id`. Record it in the run log when available.

### Unload

After repetitions, call:

```http
POST /api/v1/models/unload
Content-Type: application/json
```

Request body:

```json
{
  "instance_id": "mistralai/ministral-3-8b"
}
```

Use the actual loaded instance id resolved during load, not blindly the plan model name unless LM Studio reports that as the instance id.

Unload behavior:

- If this run loaded the model, unload it in a `finally` block.
- If `/load` fails, do not call `/unload`.
- If a repetition fails after successful load, still attempt `/unload`.
- If `/unload` fails, fail the run and make the cleanup problem visible.
- If the model was already loaded before the run and reused, do not unload it automatically.

## Package and Type Refactor

Rename:

```text
ch.thp.mas.llm.variance.client.Manufacturer
```

to:

```text
ch.thp.mas.llm.variance.client.InferenceProvider
```

Suggested enum values remain:

```text
OPENAI
ANTHROPIC
LMSTUDIO
```

Update dependent records and JSON fields:

| Current | New |
| --- | --- |
| `manufacturer` | `inferenceProvider` |
| `Manufacturer` | `InferenceProvider` |
| `manufacturer.defaultModel()` | `inferenceProvider.defaultModel()` |
| `manufacturer.createClient()` | `inferenceProvider.createClient()` |

Affected areas:

- `Plan`, `YamlPlan`, `LoadedPlan`, `ResolvedPlan`, `PlanResolver`
- `PlanBatchResolver` tests and plan fixtures
- `RunLog`, `RunLogWriter`, `RunLogReader`
- `AnalysisRunInfo`, `AnalysisResult` JSON fixtures
- `LlmClientFactory` and concrete clients
- README and specs that show plan/run examples

No backward compatibility is required:

- Existing YAML plans are migrated from `manufacturer` to `inferenceProvider`.
- Existing test fixtures and run-log examples are migrated to `inferenceProvider`.
- Run-log reading expects `inferenceProvider`.
- `manufacturer` is removed from code, tests, docs, and specs where it describes runtime selection.
- If an old plan still contains `manufacturer`, normal YAML binding or validation may fail. A dedicated compatibility error message is nice to have, but not required.

## Run Orchestration Design

Introduce a provider lifecycle abstraction instead of embedding LM Studio-specific behavior directly in `PlanRunner`.

Suggested types:

```text
ch.thp.mas.llm.variance.run
  PlanRunner
  InferenceSession
  InferenceSessionFactory
  NoopInferenceSession
  LmStudioInferenceSession

ch.thp.mas.llm.variance.client
  InferenceProvider
  LlmClient
  LlmClientFactory
  LmStudioControlClient
  LmStudioChatClient
```

`PlanRunner` should orchestrate the lifecycle generically:

```text
session = sessionFactory.open(plan)
try:
  client = session.client()
  run all repetitions
  write run log
finally:
  session.close()
```

OpenAI and Anthropic use a no-op session:

```text
open -> create client
close -> no action
```

LM Studio uses a lifecycle session:

```text
open -> check /api/v1/models -> load model if needed -> create native chat client
close -> unload model if this session loaded it
```

This keeps `PlanRunner` focused on run semantics while provider-specific lifecycle behavior lives in provider-specific infrastructure.

## Logging

Run logs should include the new field:

```json
{
  "inferenceProvider": "LMSTUDIO",
  "model": "mistralai/ministral-3-8b",
  "modelVersion": null
}
```

For LM Studio runs, add optional lifecycle metadata to make later interpretation reproducible:

```json
{
  "inferenceProvider": "LMSTUDIO",
  "model": "mistralai/ministral-3-8b",
  "modelVersion": null,
  "modelInstance": {
    "id": "mistralai/ministral-3-8b",
    "loadedByRun": true,
    "loadConfig": {
      "contextLength": 8192,
      "evalBatchSize": 512,
      "flashAttention": true,
      "numExperts": null,
      "offloadKvCacheToGpu": true
    },
    "loadResponse": {
      "status": "loaded",
      "load_config": {
        "context_length": 8192,
        "eval_batch_size": 512,
        "flash_attention": true,
        "offload_kv_cache_to_gpu": true
      }
    }
  }
}
```

If the model was already loaded and reused, `loadedByRun` is `false`.

The raw or structured model-load response must be written to the run log for LM Studio runs. This data documents how LM Studio actually loaded the model. It is operational metadata and is not part of the semantic, syntactic, or literal analysis.

Analysis behavior:

- `modelInstance.loadResponse` is read as part of the run log but not analyzed.
- It must not influence embeddings, clustering, syntactic metrics, literal metrics, or medoid selection.
- Analysis output may copy the run metadata if the existing `AnalysisRunInfo` structure is extended, but it should not compute metrics from it.

Plan/run config should also log the reasoning setting:

```json
{
  "config": {
    "temperature": 0.0,
    "topP": 1.0,
    "topK": 1,
    "seed": 1,
    "reasoning": "off"
  }
}
```

The existing MAS-required fields remain present under their new names where appropriate. The thesis term "Hersteller" can be described as the inference provider in the implementation.

## Failure Semantics

Keep the current all-or-nothing run-log behavior:

- If server check fails, no run log is written.
- If load fails, no run log is written.
- If any repetition fails, no run log is written.
- If unload fails, no run log is written, because the run did not finish cleanly.
- If log writing fails after successful unload, surface the write failure.

The only exception may be diagnostic console output. Do not create partial run JSON files unless a later spec explicitly introduces failed-run logs.

## Testing

Add focused unit tests.

### `InferenceProvider` Rename

- YAML with `inferenceProvider` resolves correctly.
- YAML with old `manufacturer` is no longer supported.
- Run-log JSON writes `inferenceProvider`.
- Run-log reading expects `inferenceProvider`.
- README examples use `inferenceProvider`.

### LM Studio Control Client

Use a lightweight HTTP test server.

Cover:

- `GET /api/v1/models` succeeds and parses available models.
- Missing model key fails with a clear exception.
- Unreachable control server fails before load.
- `POST /api/v1/models/load` sends snake_case load options and omits nulls.
- Instance id is resolved from load response when present.
- Instance id is resolved from a follow-up `GET /api/v1/models` when load does not return it.
- `POST /api/v1/models/unload` sends the resolved `instance_id`.
- Authorization header is sent when `LM_API_TOKEN` is configured.
- The raw or structured load response is exposed to `PlanRunner` for logging.

### LM Studio Native Chat Client

Use a lightweight HTTP test server.

Cover:

- `POST /api/v1/chat` sends `model`, `input`, generation parameters, `reasoning`, and `store: false`.
- Missing `reasoning` in the plan defaults to `off`.
- `reasoning: low|medium|high|on` is passed through unchanged.
- `topP` maps to `top_p`.
- `topK` maps to `top_k`.
- Null optional parameters are omitted.
- Multiple `message` output items are concatenated in response order.
- `reasoning`, `tool_call`, and `invalid_tool_call` output items do not become answer text.
- Missing message output fails the repetition.
- `stats.input_tokens` and `stats.total_output_tokens` map to internal token usage.
- `model_instance_id` is captured as provider metadata for the run log.
- Authorization header is sent when `LM_API_TOKEN` is configured.

### Plan Runner Lifecycle

Use fake sessions and fake clients.

Cover:

- OpenAI/Anthropic no-op session still executes repetitions and writes logs.
- LM Studio session checks, loads, runs all repetitions, unloads, then writes the log.
- LM Studio repetitions use `LmStudioChatClient`, not the Anthropic-compatible client.
- LM Studio run logs include the model load response and reasoning setting.
- If load fails, no repetitions happen and unload is not called.
- If repetition 2 fails, unload is still called and no run log is written.
- If unload fails after successful repetitions, the run fails visibly and no run log is written.
- If model was already loaded and reused, close does not unload it.

### Integration Boundary

Do not require a real LM Studio instance for normal tests. Any live LM Studio test should be disabled or explicitly tagged.

Suggested verification command:

```bash
gradle --no-daemon test
```

In this workspace, continue using the local JDK setup when needed:

```powershell
$env:JAVA_HOME='C:\develop\jdk-21.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME='C:\Users\thier\.gradle'
```

## Open Questions

- Should LM Studio reuse already loaded instances by default, or should a plan be able to force a fresh reload?
- Should LM Studio load configuration become part of every plan, or only of LM Studio-specific plans?
- Should failed runs get a separate diagnostic log in the future, distinct from successful MAS run logs?
