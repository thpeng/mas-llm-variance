# LLM Variance

`mas-llm-variance` is a small Java/Spring Boot research tool for running controlled LLM experiment series and analyzing how much the generated answers vary.

The project is built around one core question:

> If the same prompt is executed repeatedly under controlled model parameters, how stable are the answers?

## What It Does

The application supports three main workflows:

1. Define experiment plans as YAML files.
2. Execute one or more plans and write run logs.
3. Analyze completed run logs for semantic, syntactic, and literal variance.

Plans live in:

```text
src/main/resources/plans/
```

Run logs are written to:

```text
src/main/resources/runs/
```

Analysis results are written to:

```text
src/main/resources/analysis/
```

Run and analysis artifacts are intended to be committed manually to GitHub. The larger research workflow assumes GitHub rulesets, signed commits, linear history, deletion protection, and manual Git tags for experiment executions.

## Project Structure

```text
src/main/java/ch/thp/mas/llm/variance/
  LlmVarianceApplication.java
  client/
  plan/
  run/
  analyze/
```

### `client`

Provider abstraction for LLM calls.

Implemented providers:

- OpenAI
- Anthropic
- Google Gemini
- LM Studio through the native developer REST API

All provider integrations use explicit HTTP request paths instead of vendor SDKs. This keeps request URL and header logging consistent across providers.

The `LlmClient` abstraction returns an `LlmResponse`, including:

- Full generated text
- Token usage where the provider exposes it
- Sanitized request trace metadata for the run log

### `plan`

Plan loading and resolution.

Operative plans are YAML files under `src/main/resources/plans` named with a four-digit prefix:

```text
0001-<scenario-name>.yml
0002-<scenario-name>.yml
```

The prefix gives plans a natural execution order. The CLI supports:

```bash
./gradlew bootRun --args="--run=plans"
./gradlew bootRun --args="--run=plans/<subfolder>"
./gradlew bootRun --args="--run=0001-<scenario-name>"
./gradlew bootRun --args="--run=plans/<subfolder>/0001-<scenario-name>.yml"
```

`run` is the command that turns plan YAML files into run logs. Folder selections are recursive. A single plan can be selected by file path or by basename. If a basename or folder name matches multiple files or folders, the command fails instead of guessing.

A plan defines top-level scenario metadata plus explicit `run` and `analysis` blocks. The `run` block contains the prompt, iteration count, temperature, top-p, top-k, seed, and reasoning setting. `seed` may be numeric or `RANDOM`, where `RANDOM` lets the provider/model choose the seed.

- Inference provider
- Model
- Description
- Run configuration
- Analysis configuration

### `run`

Execution and run logging.

For each resolved plan, the runner:

1. Opens an inference session.
2. Loads the LM Studio model when the selected provider requires an explicit lifecycle.
3. Executes the prompt for the configured number of iterations.
4. Captures start/end timestamps for every repetition.
5. Captures the sanitized request URL and request headers, excluding authentication.
6. Captures the full answer and token usage.
7. Unloads LM Studio models loaded by the run.
8. Writes one JSON run log if and only if the full plan succeeds.

Run log files are timestamped:

```text
yyyyMMdd-HHmmss-SSS-run-<plan-name>.json
```

The run logger is intentionally all-or-nothing. If one repetition fails, no partial log is written.

### `analyze`

Analysis of completed run logs.

The analyzer reads JSON run logs, derives the matching plan YAML from each run log's `planName`, and produces JSON analysis files. It is deliberately separated from execution so experiments can be run first and analyzed later. The plan YAML is required because it contains the `analysis` configuration used for semantic clustering and metric parameters.

CLI:

```bash
./gradlew bootRun --args="--analyze=runs"
./gradlew bootRun --args="--analyze=runs/<subfolder>"
./gradlew bootRun --args="--analyze=<run-log-file-name>"
./gradlew bootRun --args="--analyze=runs/<subfolder>/<run-log-file-name>.json"
```

`analyze` is the command that turns run logs into analysis files. Folder selections are recursive. A single run log can be selected by file path or by basename. If a basename or folder name matches multiple files or folders, the command fails instead of guessing.

Run mode and analyze mode are mutually exclusive in behavior: run mode executes plans; analyze mode evaluates existing run logs against the plan YAML named in the run log.

## Main Analysis Algorithm

The analysis has three layers: semantic analysis, syntactic analysis, and literal analysis.

### 1. Semantic Analysis

The semantic analysis follows this pipeline:

1. Read all responses from a run log.
2. Transform each response into an embedding.
3. Compute pairwise cosine distances between embeddings.
4. Select the medoid: the response with the lowest total distance to all other responses.
5. Build an inclusive scan range for the selected clustering algorithm.
6. Cluster responses once per scan value with hierarchical clustering by default, or DBSCAN when explicitly configured.
7. Report one semantic and syntactic result per scan value, including the cluster count, plus one literal result for the whole run.

The medoid is the typical answer in the run. It is not a correctness reference.

Hierarchical clustering is the default because it avoids DBSCAN-style chaining effects in longer generated texts. It is configured with complete linkage by default, so two clusters are merged only when the most distant response pair still stays below the configured threshold. DBSCAN remains available for epsilon scans and explicit outlier detection when that behavior is desired.

The scan does not choose the best threshold automatically. It makes cluster stability visible across nearby parameter values; interpreting plateaus or selecting an operational value is a downstream analysis step.

Example analysis configuration:

```yaml
analysis:
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

Current note: the executable implementation integrates with a WSL-hosted FastAPI service for `intfloat/multilingual-e5-large` on `http://localhost:8000`. A deterministic local hashing embedding service remains available for development and tests via `LLM_VARIANCE_EMBEDDING_PROVIDER=local-hashing`, but it is not a real semantic model.

### 2. Syntactic Analysis

After semantic clustering, syntactic variance is measured inside each semantic cluster.

For every pair of responses in a cluster:

- Compute ROUGE-L F1 similarity.
- Compute sentence-level BLEU with Chen & Cherry method-1 smoothing.
- Convert similarities to distances with `1.0 - score`.

Then aggregate per cluster:

- Median ROUGE-L distance
- p90 ROUGE-L distance
- Median BLEU distance
- p90 BLEU distance

This pairwise approach avoids the need for a reference answer. It supports relative variance comparison inside one run, but it does not measure factual correctness or human preference.

## Configuration And Environment

Provider environment variables:

```text
OPENAI_API_KEY
ANTHROPIC_API_KEY
GOOGLE_API_KEY
LMSTUDIO_BASE_URL
LM_API_TOKEN
```

`LMSTUDIO_BASE_URL` is optional and defaults to `http://127.0.0.1:10022`. LM Studio uses `/api/v1/models`, `/api/v1/models/load`, `/api/v1/chat`, and `/api/v1/models/unload`. `LM_API_TOKEN` is optional.

Embedding environment variables:

```text
LLM_VARIANCE_EMBEDDING_PROVIDER=e5-http
LLM_VARIANCE_EMBEDDING_BASE_URL=http://localhost:8000
```

The E5 model is served by `src/main/python/server.py` inside WSL. Java calls `/load`, `/embed`, and `/unload` during analysis. Use `LLM_VARIANCE_EMBEDDING_PROVIDER=local-hashing` only when the WSL model server is unavailable for development or tests.

## Testing

Run the test suite with:

```bash
./gradlew test
```


## Specs

Design and increment specs live in:

```text
specs/
```
