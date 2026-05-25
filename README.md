# LLM Variance

`mas-llm-variance` is a small Java/Spring Boot research tool for running controlled LLM experiment series and analyzing how much the generated answers vary.

The project is built around one core question:

> If the same prompt is executed repeatedly under controlled model parameters, how stable are the answers?

## What It Does

The application supports four main workflows:

1. Define experiment plans as YAML files.
2. Execute one or more plans and write run logs.
3. Analyze completed run logs for semantic, syntactic, and literal variance.
4. Aggregate analysis JSON files into a compact CSV for downstream statistics.

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

Meta-analysis CSV exports are written to:

```text
src/main/resources/metanalysis/
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
  metanalysis/
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

A plan defines top-level scenario metadata plus explicit `run` and `analysis` blocks. The `run` block contains the prompt, iteration count, temperature, top-p, top-k, seed, and reasoning setting. `seed` may be numeric or `RANDOM`, where `RANDOM` generates and logs a fresh request seed for every repetition. LM Studio does not support seed configuration in this tool; configuring a seed for LM Studio plans fails fast.

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
5. Captures sanitized request URL, request headers, request body, response status, response headers, and response body.
6. Captures the full parsed answer and token usage.
7. Unloads LM Studio models loaded by the run.
8. Writes one JSON run log.

Run log files are timestamped:

```text
yyyyMMdd-HHmmss-SSS-run-<plan-name>.json
```

Provider errors with HTTP 5xx status are treated as serving errors. They are written as failed repetitions and counted in the run log, while the run continues with the next repetition. Non-5xx request errors still fail the run because they usually indicate invalid configuration.

Each run log also contains an execution environment snapshot with Git commit, branch, dirty state, Java/OS runtime information, and GPU information where available. For batch execution, this environment snapshot is captured once at command start and reused for all run logs produced by that command.

### `analyze`

Analysis of completed run logs.

The analyzer reads JSON run logs, derives the matching plan YAML from each run log's `planName`, and produces JSON analysis files. It is deliberately separated from execution so experiments can be run first and analyzed later. The plan YAML is required because it contains the `analysis` configuration used for the prompt-specific evaluation and metric parameters.

CLI:

```bash
./gradlew bootRun --args="--analyze=runs"
./gradlew bootRun --args="--analyze=runs/<subfolder>"
./gradlew bootRun --args="--analyze=<run-log-file-name>"
./gradlew bootRun --args="--analyze=runs/<subfolder>/<run-log-file-name>.json"
```

`analyze` is the command that turns run logs into analysis files. Folder selections are recursive. A single run log can be selected by file path or by basename. If a basename or folder name matches multiple files or folders, the command fails instead of guessing.

Run mode and analyze mode are mutually exclusive in behavior: run mode executes plans; analyze mode evaluates existing run logs against the plan YAML named in the run log.

### `metanalysis`

CSV export over completed analysis files.

The command reads one analysis file or a recursive folder selection under `src/main/resources/analysis`, resolves the linked run log for each analysis, and writes one CSV row per experiment series.

CLI:

```bash
./gradlew bootRun --args="--metanalysis=analysis"
./gradlew bootRun --args="--metanalysis=analysis/<subfolder>"
./gradlew bootRun --args="--metanalysis=<analysis-file-name>"
./gradlew bootRun --args="--metanalysis=analysis/<subfolder>/<analysis-file-name>.json"
./gradlew bootRun --args="--metanalysis=analysis/main_100_iterations --metanalysis-output=src/main/resources/metanalysis/main_100_iterations.csv"
```

Folder selections are recursive. A single analysis can be selected by file path or by basename. If a basename or folder name matches multiple files or folders, the command fails instead of guessing.

The exported CSV contains series metadata, prompt archetype, prompt language, sampling settings, success/failure counts, semantic validity/outlier rates, literal stability, largest semantic cluster share, BLEU/ROUGE distance summaries, token totals, token p10/median/p90 values, reasoning-token totals, and client-side duration totals plus p10/median/p90 values.

## Main Analysis Approach

The analysis no longer uses a generic embedding clustering algorithm. It evaluates responses through prompt-specific evaluation paths derived from the four prompt archetypes used in the research design. These evaluations are configured with:

```yaml
analysis:
  promptEvaluation: ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
```

Supported values:

- `ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP`
- `FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION`
- `LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE`
- `CREATIVE_GENERATIVE_LUCERNE_MARKETING`

The analysis result contains the selected archetype evaluation, plus generic literal stability. Syntactic BLEU/ROUGE analysis is computed for those archetype evaluations where it is meaningful.

### 1. Advisory Recommendation: Swiss Round Trip

This path evaluates the advisory/recommendation archetype for Swiss round trips. It extracts five numbered station names from each response, normalizes them to a curated Swiss destination enum, and treats the ordered route as the central semantic content.

It reports:

- successful, partial, and failed extractions
- unknown destination names
- unique route count and top route share
- route clusters by exact normalized route key
- station frequencies
- position distributions
- pairwise Jaccard statistics over route destination sets
- syntactic BLEU/ROUGE distances inside each route cluster

Example:

```yaml
analysis:
  promptEvaluation: ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
  swissRoundTrip:
    expectedStationCount: 5
    language: DE
  bleu:
    maxN: 4
    smoothingEpsilon: 0.1
  rouge:
    variant: ROUGE_L
    aggregation: F1
```

### 2. Factual Critical: Bern-Zurich Connection

This path evaluates the factual-critical archetype. It checks whether each response contains the expected departure time from Bern, expected arrival time at Zurich HB, and zero changes.

It normalizes time formats such as `8:02`, `08.02`, and `09.15` to `HH:mm`, detects extra distractor times, and recognizes German expressions for a direct/no-change connection. Responses that do not satisfy all factual conditions are reported as outliers with failure reasons.

Example:

```yaml
analysis:
  promptEvaluation: FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION
  bernZurichConnection:
    departureFromBern: "08:02"
    arrivalAtZurich: "09:15"
    changes: 0
```

### 3. Literal Format Critical: Traveler Guidance

This path evaluates the literal-format-critical archetype. It compares each answer against a configured reference sentence.

It reports exact matches, whitespace-normalized exact matches, and non-matches. Whitespace normalization only collapses whitespace; it does not normalize case, punctuation, spelling, or wording. Non-matches include diagnostic flags for missing target terms, leaked template content, and additional sentence candidates.

Example:

```yaml
analysis:
  promptEvaluation: LITERAL_FORMAT_CRITICAL_TRAVELER_GUIDANCE
  travelerGuidanceFormat:
    reference: "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3."
```

### 4. Creative Generative: Lucerne Marketing Text

This path evaluates the creative-generative archetype with deliberately lightweight semantic constraints. It checks whether the response contains the required term and has the expected number of sentences. Responses that satisfy both conditions are accepted for syntactic comparison; other responses are reported as outliers.

Example:

```yaml
analysis:
  promptEvaluation: CREATIVE_GENERATIVE_LUCERNE_MARKETING
  lucerneMarketingText:
    expectedSentenceCount: 3
    requiredTerm: Luzern
  bleu:
    maxN: 4
    smoothingEpsilon: 0.1
  rouge:
    variant: ROUGE_L
    aggregation: F1
```

### 5. Syntactic Analysis

Where applicable, syntactic variance is measured inside the successful semantic group or route clusters produced by the prompt-specific evaluator.

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

### 6. Literal Analysis

Generic literal analysis is always computed over all successful raw responses, independently of the selected prompt evaluation. It reports whether all responses are identical, how many distinct responses occurred, and the exact-match rate inside the run.

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
