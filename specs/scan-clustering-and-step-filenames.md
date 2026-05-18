# Scan Clustering and Step File Names

## Goal

Two improvements are required for the run and analysis pipeline:

1. Run and analysis file names must always include the processing step: `run` for execution logs and `analyze` for analysis files.
2. DBSCAN and hierarchical clustering must no longer use a single configured epsilon or threshold. Instead, the configured value becomes an inclusive range. The analysis scans every value in that range with a configurable increment (default `0.01`).

For every scan value, the application must calculate a distinct semantic cluster structure and a distinct syntactic analysis. Literal analysis remains a single result for the whole run, because it does not depend on semantic clustering.

## Scope and Non-Goals

This spec covers the **generation** of scan data: producing, for one run, a set of clustering results across a range of nearby parameter values.

This spec explicitly does **not** cover the **interpretation** of scan data. Selecting a representative threshold, detecting a stability plateau, or deriving a per-run variance verdict is a separate downstream analysis step and is out of scope here. The scan produces the raw material for that step; it does not perform it.

This separation is intentional. The scan must not auto-select a "best" value. Its purpose is to make cluster (in)stability across nearby parameter values visible and inspectable.

## File Name Convention

The two step markers are the pipeline command names: `run` (produce an execution log) and `analyze` (produce an analysis from a log). Both are used as verbs / command names, and both are applied consistently. This is the reason `analyze` is used rather than `analysis`.

### Run Files

Current run files are produced by `RunFileNameFactory` and look like:

```text
20260518-133439-722-0001-google-flash-rundreise.json
```

They must include the step marker `run`:

```text
20260518-133439-722-run-0001-google-flash-rundreise.json
```

Rules:

- Keep the timestamp prefix.
- Insert `run` directly after the timestamp.
- Keep the sanitized plan name.
- Keep `.json`.
- The marker must be lowercase and literal: `run`.

### Analyze Files

Current analysis files are derived from the run log name and include `analysis`:

```text
20260518-133439-722-0001-google-flash-rundreise-analysis-20260518-134814-903.json
```

They must include the step marker `analyze`:

```text
20260518-133439-722-run-0001-google-flash-rundreise-analyze-20260518-134814-903.json
```

Rules:

- Keep deriving the analysis filename from the source run filename.
- Append `analyze` and the analysis timestamp.
- Use `analyze`, not `analysis` (see the step-marker rationale above).
- If the source run filename already contains `run`, keep it. Do not try to strip or rewrite historical source names beyond removing `.json`.

## YAML Configuration

The analysis block should describe scan ranges instead of a single threshold value.

### DBSCAN

Replace:

```yaml
dbscan:
  epsilon: 0.06
  minPts: 2
```

With:

```yaml
dbscan:
  epsilon:
    from: 0.05
    to: 0.15
  minPts: 2
```

### Hierarchical

Replace:

```yaml
hierarchical:
  threshold: 0.08
  linkage: COMPLETE
```

With:

```yaml
hierarchical:
  threshold:
    from: 0.03
    to: 0.12
  linkage: COMPLETE
```

### Increment

The scan increment has a default of `0.01` and is configurable in the YAML:

```yaml
analysis:
  scanIncrement: 0.01
```

If `scanIncrement` is omitted, `0.01` is used. The increment must always be reported in the analysis output config block, whether default or explicit.

## Range Semantics

Range values are inclusive. With increment `0.01`:

```text
from = 0.03
to   = 0.06
```

produces:

```text
0.03, 0.04, 0.05, 0.06
```

Implementation rules:

- Represent range bounds and scan values internally as **integer hundredths** (or another fixed-point integer representation matching the increment granularity). Do not perform range iteration, comparison, or alignment checks on `double`. `double` is produced only at the final JSON serialization boundary.
- The scan increment defines the granularity. All validation below is relative to the configured increment, not hardcoded to hundredths.
- Round/format scan values to two decimal places in the JSON output.
- Reject negative range bounds.
- Reject `from > to`.
- Reject bounds that do not align to the configured increment. With increment `0.01`, a bound such as `0.055` is rejected. With increment `0.02`, bounds must be multiples of `0.02` offset from `from`. If a future spec adds a rounding policy, this rule is revisited.
- `from == to` is a valid, deliberately supported degenerate scan: it produces exactly one scan value and reproduces the previous single-value behaviour. This is the basis for the defaults below.
- Keep DBSCAN `minPts` as a single value shared by all epsilon scans.
- Keep hierarchical `linkage` as a single value shared by all threshold scans.

## DBSCAN vs Hierarchical: Scan Semantics Are Not Symmetric

For hierarchical clustering, `threshold` is the single distance-governing parameter, so scanning it yields a clean parameter-stability curve.

For DBSCAN, `epsilon` is **not** the only structural parameter; `minPts` co-determines the result. This spec scans `epsilon` only, with `minPts` held fixed. A DBSCAN scan therefore shows epsilon-stability **at a fixed minPts**, not full parameter stability.

Implication for the output: the fixed `minPts` value MUST be recorded in the analysis output (in the config block, and it is acceptable to also surface it per scan entry) so that a DBSCAN scan file is self-describing without reference to the source plan.

## Analysis Result Shape

The current result shape has one semantic analysis and one syntactic analysis:

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

This should be refactored so clustering-dependent results are represented as scan entries:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        LiteralAnalysis literal
) {
}
```

Suggested scan record:

```java
public record AnalysisScan(
        ClusteringAlgorithm algorithm,
        String parameter,
        double value,
        int clusterCount,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic
) {
}
```

The `scans` list MUST be ordered ascending by `value`. This ordering is part of the contract so that golden-JSON tests and downstream stability analysis can rely on it.

Examples:

```json
{
  "algorithm": "HIERARCHICAL",
  "parameter": "threshold",
  "value": 0.06,
  "clusterCount": 3,
  "semantic": { },
  "syntactic": { }
}
```

```json
{
  "algorithm": "DBSCAN",
  "parameter": "epsilon",
  "value": 0.15,
  "clusterCount": 2,
  "semantic": { },
  "syntactic": { }
}
```

The existing `SemanticAnalysis` can still contain medoid, pairwise distance statistics, clusters, and outliers. Medoid and pairwise distance statistics will be identical across scan entries for the same run/config, but duplicating them keeps each scan self-contained and easy to inspect.

## Algorithm Flow

The analysis flow becomes:

1. Read run log and matching plan YAML.
2. Extract raw responses.
3. Compute embeddings and the pairwise semantic distance matrix once.
4. Compute medoid and pairwise distance summary once.
5. Build the scan values for the selected clustering algorithm:
    - `DBSCAN`: `dbscan.epsilon.from` to `dbscan.epsilon.to`, inclusive, stepped by the configured increment.
    - `HIERARCHICAL`: `hierarchical.threshold.from` to `hierarchical.threshold.to`, inclusive, stepped by the configured increment.
6. For each scan value (ascending):
    - Build a temporary clustering config with that value.
    - Cluster the same distance matrix.
    - Build semantic clusters and outliers for that value.
    - Compute syntactic analysis using that value's semantic clusters.
    - Append an `AnalysisScan`.
7. Compute literal analysis once across all raw responses.
8. Write one analysis JSON containing all scans (ascending by value) and the single literal result.

The selected `analysis.clusteringAlgorithm` still controls which algorithm is scanned. We do not scan both DBSCAN and hierarchical in one analysis unless a future spec introduces an `ALL` or list mode for algorithms.

## Config Model Changes

Introduce a small reusable range record. Internally it stores integer hundredths; `double` accessors exist only for serialization:

```java
public record ScanRange(int fromHundredths, int toHundredths) {
    // validation: fromHundredths >= 0, fromHundredths <= toHundredths
    // double from() / double to() provided for JSON output only
}
```

Refactor:

```java
public record DbscanConfig(double epsilon, int minPts)
```

to:

```java
public record DbscanConfig(ScanRange epsilon, int minPts)
```

Refactor:

```java
public record HierarchicalConfig(double threshold, HierarchicalLinkage linkage)
```

to:

```java
public record HierarchicalConfig(ScanRange threshold, HierarchicalLinkage linkage)
```

The clusterer implementations should still receive a concrete scalar value. Either:

- add overloads accepting the scalar value directly, or
- create a per-scan scalar config internally, for example `DbscanRunConfig(double epsilon, int minPts)`.

Prefer keeping `DbscanClusterer` and `HierarchicalClusterer` focused on a single clustering operation. The scan loop should live in `Analyzer` or a small helper such as `ClusterScanRunner`.

## YAML Mapper Changes

Update `YamlAnalysisConfig`:

```java
public static class Dbscan {
    private Range epsilon;
    private Integer minPts;
}

public static class Hierarchical {
    private Range threshold;
    private HierarchicalLinkage linkage;
}

public static class Range {
    private Double from;
    private Double to;
}
```

Update `AnalysisConfigMapper`:

- map `dbscan.epsilon.from` and `dbscan.epsilon.to`
- map `hierarchical.threshold.from` and `hierarchical.threshold.to`
- map `analysis.scanIncrement`, defaulting to `0.01` when absent
- convert mapped `double` bounds to integer hundredths at mapping time, applying the increment-alignment validation; reject misaligned bounds with a clear message
- fail clearly if one side of a range is missing (`from` or `to` absent)
- fail clearly if a clustering parameter is supplied as an old-style scalar (`epsilon: 0.06` instead of an `epsilon:` block). SnakeYAML will not map a scalar into the `Range` object cleanly; the mapper MUST detect this and produce an explicit, human-readable error naming the parameter and the expected `from`/`to` structure, rather than surfacing a SnakeYAML stacktrace or silently producing a null range.
- preserve defaults by converting old scalar defaults into ranges with identical `from` and `to` only inside `AnalysisConfig.defaults()`

Default examples (degenerate single-value scans, see Range Semantics):

```java
new DbscanConfig(ScanRange.ofHundredths(15, 15), 2)
new HierarchicalConfig(ScanRange.ofHundredths(8, 8), linkage)
```

## Backward Compatibility

The project is still in verification. No compatibility with old scalar YAML is required.

Recommendation:

- Main plan YAML should use the new range structure.
- The mapper must still fail *cleanly* on old scalar YAML (see Mapper Changes); this is a defined, tested failure, not undefined behaviour.
- Existing generated run/analysis files do not need migration.

## Testing Plan

### File Name Tests

`RunFileNameFactoryTest`

- `create(timestamp, "0001-test")` returns `yyyyMMdd-HHmmss-SSS-run-0001-test.json`.
- plan names are still sanitized.

`AnalysisFileNameFactoryTest`

- source `20260518-120000-000-run-0001-test.json` produces `20260518-120000-000-run-0001-test-analyze-yyyyMMdd-HHmmss-SSS.json`.
- source without `.json` still works.

### Range Tests

`ScanRangeTest` or `ClusterScanValuesTest`

- `0.03..0.06` (increment `0.01`) produces `0.03, 0.04, 0.05, 0.06`.
- `0.06..0.06` produces exactly one value (degenerate scan).
- negative bounds are rejected.
- `from > to` is rejected.
- bounds misaligned to the increment are rejected (`0.055` with increment `0.01`).
- a non-default increment (e.g. `0.02`) produces correctly stepped values, and bounds misaligned to that increment are rejected.
- scan values are produced in ascending order.

### Mapper Tests

`AnalysisConfigMapperTest`

- maps DBSCAN epsilon range.
- maps hierarchical threshold range.
- maps `scanIncrement`, and defaults it to `0.01` when absent.
- rejects missing `from`.
- rejects missing `to`.
- rejects an old-style scalar `epsilon`/`threshold` with a clear, parameter-named error message.

### Analyzer Tests

`AnalyzerTest`

- with hierarchical range `0.03..0.05`, result contains three scan entries.
- each scan entry has `parameter = "threshold"` and values `0.03`, `0.04`, `0.05` in ascending order.
- syntactic analysis is calculated separately per scan, based on that scan's clusters.
- literal analysis is present once at top level.
- DBSCAN range similarly produces one scan per epsilon, and the fixed `minPts` is recorded in the output config.

### Golden JSON Tests

Update the existing expected analysis JSON fixture:

- replace top-level `semantic` and `syntactic` with `scans`
- assert `literal` remains top-level
- assert `scans` is ascending by `value`
- `value` is serialized as a JSON number. Tests MUST compare it numerically (with a small tolerance), not as an exact string. JSON has no decimal-places semantics, so `0.06` vs `0.060` vs a float-formatted variant are not string-stable across serializer versions. The "two decimal places" requirement is a display/formatting concern at the serialization boundary, not a JSON-level guarantee, and tests must not assert it via string equality.

## Documentation Updates

Update:

- `README.md` analysis section
- `specs/yaml-analysis-config-plan-pairing.md`
- any sample plans under `src/main/resources/plans` and `src/test/resources/plans`

The documentation should make clear that the scan is intended to make cluster stability visible across nearby thresholds, not to automatically choose the "best" threshold, and that interpreting the scan (plateau detection, representative-value selection) is a separate downstream step outside this spec.
# Scan Clustering and Step File Names

## Goal

Two improvements are required for the run and analysis pipeline:

1. Run and analysis file names must always include the processing step: `run` for execution logs and `analyze` for analysis files.
2. DBSCAN and hierarchical clustering must no longer use a single configured epsilon or threshold. Instead, the configured value becomes an inclusive range. The analysis scans every value in that range with a configurable increment (default `0.01`).

For every scan value, the application must calculate a distinct semantic cluster structure and a distinct syntactic analysis. Literal analysis remains a single result for the whole run, because it does not depend on semantic clustering.

## Scope and Non-Goals

This spec covers the **generation** of scan data: producing, for one run, a set of clustering results across a range of nearby parameter values.

This spec explicitly does **not** cover the **interpretation** of scan data. Selecting a representative threshold, detecting a stability plateau, or deriving a per-run variance verdict is a separate downstream analysis step and is out of scope here. The scan produces the raw material for that step; it does not perform it.

This separation is intentional. The scan must not auto-select a "best" value. Its purpose is to make cluster (in)stability across nearby parameter values visible and inspectable.

## File Name Convention

The two step markers are the pipeline command names: `run` (produce an execution log) and `analyze` (produce an analysis from a log). Both are used as verbs / command names, and both are applied consistently. This is the reason `analyze` is used rather than `analysis`.

### Run Files

Current run files are produced by `RunFileNameFactory` and look like:

```text
20260518-133439-722-0001-google-flash-rundreise.json
```

They must include the step marker `run`:

```text
20260518-133439-722-run-0001-google-flash-rundreise.json
```

Rules:

- Keep the timestamp prefix.
- Insert `run` directly after the timestamp.
- Keep the sanitized plan name.
- Keep `.json`.
- The marker must be lowercase and literal: `run`.

### Analyze Files

Current analysis files are derived from the run log name and include `analysis`:

```text
20260518-133439-722-0001-google-flash-rundreise-analysis-20260518-134814-903.json
```

They must include the step marker `analyze`:

```text
20260518-133439-722-run-0001-google-flash-rundreise-analyze-20260518-134814-903.json
```

Rules:

- Keep deriving the analysis filename from the source run filename.
- Append `analyze` and the analysis timestamp.
- Use `analyze`, not `analysis` (see the step-marker rationale above).
- If the source run filename already contains `run`, keep it. Do not try to strip or rewrite historical source names beyond removing `.json`.

## YAML Configuration

The analysis block should describe scan ranges instead of a single threshold value.

### DBSCAN

Replace:

```yaml
dbscan:
  epsilon: 0.06
  minPts: 2
```

With:

```yaml
dbscan:
  epsilon:
    from: 0.05
    to: 0.15
  minPts: 2
```

### Hierarchical

Replace:

```yaml
hierarchical:
  threshold: 0.08
  linkage: COMPLETE
```

With:

```yaml
hierarchical:
  threshold:
    from: 0.03
    to: 0.12
  linkage: COMPLETE
```

### Increment

The scan increment has a default of `0.01` and is configurable in the YAML:

```yaml
analysis:
  scanIncrement: 0.01
```

If `scanIncrement` is omitted, `0.01` is used. The increment must always be reported in the analysis output config block, whether default or explicit.

## Range Semantics

Range values are inclusive. With increment `0.01`:

```text
from = 0.03
to   = 0.06
```

produces:

```text
0.03, 0.04, 0.05, 0.06
```

Implementation rules:

- Represent range bounds and scan values internally as **integer hundredths** (or another fixed-point integer representation matching the increment granularity). Do not perform range iteration, comparison, or alignment checks on `double`. `double` is produced only at the final JSON serialization boundary.
- The scan increment defines the granularity. All validation below is relative to the configured increment, not hardcoded to hundredths.
- Round/format scan values to two decimal places in the JSON output.
- Reject negative range bounds.
- Reject `from > to`.
- Reject bounds that do not align to the configured increment. With increment `0.01`, a bound such as `0.055` is rejected. With increment `0.02`, bounds must be multiples of `0.02` offset from `from`. If a future spec adds a rounding policy, this rule is revisited.
- `from == to` is a valid, deliberately supported degenerate scan: it produces exactly one scan value and reproduces the previous single-value behaviour. This is the basis for the defaults below.
- Keep DBSCAN `minPts` as a single value shared by all epsilon scans.
- Keep hierarchical `linkage` as a single value shared by all threshold scans.

## DBSCAN vs Hierarchical: Scan Semantics Are Not Symmetric

For hierarchical clustering, `threshold` is the single distance-governing parameter, so scanning it yields a clean parameter-stability curve.

For DBSCAN, `epsilon` is **not** the only structural parameter; `minPts` co-determines the result. This spec scans `epsilon` only, with `minPts` held fixed. A DBSCAN scan therefore shows epsilon-stability **at a fixed minPts**, not full parameter stability.

Implication for the output: the fixed `minPts` value MUST be recorded in the analysis output (in the config block, and it is acceptable to also surface it per scan entry) so that a DBSCAN scan file is self-describing without reference to the source plan.

## Analysis Result Shape

The current result shape has one semantic analysis and one syntactic analysis:

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

This should be refactored so clustering-dependent results are represented as scan entries:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        LiteralAnalysis literal
) {
}
```

Suggested scan record:

```java
public record AnalysisScan(
        ClusteringAlgorithm algorithm,
        String parameter,
        double value,
        int clusterCount,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic
) {
}
```

The `scans` list MUST be ordered ascending by `value`. This ordering is part of the contract so that golden-JSON tests and downstream stability analysis can rely on it.

Examples:

```json
{
  "algorithm": "HIERARCHICAL",
  "parameter": "threshold",
  "value": 0.06,
  "clusterCount": 3,
  "semantic": { },
  "syntactic": { }
}
```

```json
{
  "algorithm": "DBSCAN",
  "parameter": "epsilon",
  "value": 0.15,
  "clusterCount": 2,
  "semantic": { },
  "syntactic": { }
}
```

The existing `SemanticAnalysis` can still contain medoid, pairwise distance statistics, clusters, and outliers. Medoid and pairwise distance statistics will be identical across scan entries for the same run/config, but duplicating them keeps each scan self-contained and easy to inspect.

## Algorithm Flow

The analysis flow becomes:

1. Read run log and matching plan YAML.
2. Extract raw responses.
3. Compute embeddings and the pairwise semantic distance matrix once.
4. Compute medoid and pairwise distance summary once.
5. Build the scan values for the selected clustering algorithm:
    - `DBSCAN`: `dbscan.epsilon.from` to `dbscan.epsilon.to`, inclusive, stepped by the configured increment.
    - `HIERARCHICAL`: `hierarchical.threshold.from` to `hierarchical.threshold.to`, inclusive, stepped by the configured increment.
6. For each scan value (ascending):
    - Build a temporary clustering config with that value.
    - Cluster the same distance matrix.
    - Build semantic clusters and outliers for that value.
    - Compute syntactic analysis using that value's semantic clusters.
    - Append an `AnalysisScan`.
7. Compute literal analysis once across all raw responses.
8. Write one analysis JSON containing all scans (ascending by value) and the single literal result.

The selected `analysis.clusteringAlgorithm` still controls which algorithm is scanned. We do not scan both DBSCAN and hierarchical in one analysis unless a future spec introduces an `ALL` or list mode for algorithms.

## Config Model Changes

Introduce a small reusable range record. Internally it stores integer hundredths; `double` accessors exist only for serialization:

```java
public record ScanRange(int fromHundredths, int toHundredths) {
    // validation: fromHundredths >= 0, fromHundredths <= toHundredths
    // double from() / double to() provided for JSON output only
}
```

Refactor:

```java
public record DbscanConfig(double epsilon, int minPts)
```

to:

```java
public record DbscanConfig(ScanRange epsilon, int minPts)
```

Refactor:

```java
public record HierarchicalConfig(double threshold, HierarchicalLinkage linkage)
```

to:

```java
public record HierarchicalConfig(ScanRange threshold, HierarchicalLinkage linkage)
```

The clusterer implementations should still receive a concrete scalar value. Either:

- add overloads accepting the scalar value directly, or
- create a per-scan scalar config internally, for example `DbscanRunConfig(double epsilon, int minPts)`.

Prefer keeping `DbscanClusterer` and `HierarchicalClusterer` focused on a single clustering operation. The scan loop should live in `Analyzer` or a small helper such as `ClusterScanRunner`.

## YAML Mapper Changes

Update `YamlAnalysisConfig`:

```java
public static class Dbscan {
    private Range epsilon;
    private Integer minPts;
}

public static class Hierarchical {
    private Range threshold;
    private HierarchicalLinkage linkage;
}

public static class Range {
    private Double from;
    private Double to;
}
```

Update `AnalysisConfigMapper`:

- map `dbscan.epsilon.from` and `dbscan.epsilon.to`
- map `hierarchical.threshold.from` and `hierarchical.threshold.to`
- map `analysis.scanIncrement`, defaulting to `0.01` when absent
- convert mapped `double` bounds to integer hundredths at mapping time, applying the increment-alignment validation; reject misaligned bounds with a clear message
- fail clearly if one side of a range is missing (`from` or `to` absent)
- fail clearly if a clustering parameter is supplied as an old-style scalar (`epsilon: 0.06` instead of an `epsilon:` block). SnakeYAML will not map a scalar into the `Range` object cleanly; the mapper MUST detect this and produce an explicit, human-readable error naming the parameter and the expected `from`/`to` structure, rather than surfacing a SnakeYAML stacktrace or silently producing a null range.
- preserve defaults by converting old scalar defaults into ranges with identical `from` and `to` only inside `AnalysisConfig.defaults()`

Default examples (degenerate single-value scans, see Range Semantics):

```java
new DbscanConfig(ScanRange.ofHundredths(15, 15), 2)
new HierarchicalConfig(ScanRange.ofHundredths(8, 8), linkage)
```

## Backward Compatibility

The project is still in verification. No compatibility with old scalar YAML is required.

Recommendation:

- Main plan YAML should use the new range structure.
- The mapper must still fail *cleanly* on old scalar YAML (see Mapper Changes); this is a defined, tested failure, not undefined behaviour.
- Existing generated run/analysis files do not need migration.

## Testing Plan

### File Name Tests

`RunFileNameFactoryTest`

- `create(timestamp, "0001-test")` returns `yyyyMMdd-HHmmss-SSS-run-0001-test.json`.
- plan names are still sanitized.

`AnalysisFileNameFactoryTest`

- source `20260518-120000-000-run-0001-test.json` produces `20260518-120000-000-run-0001-test-analyze-yyyyMMdd-HHmmss-SSS.json`.
- source without `.json` still works.

### Range Tests

`ScanRangeTest` or `ClusterScanValuesTest`

- `0.03..0.06` (increment `0.01`) produces `0.03, 0.04, 0.05, 0.06`.
- `0.06..0.06` produces exactly one value (degenerate scan).
- negative bounds are rejected.
- `from > to` is rejected.
- bounds misaligned to the increment are rejected (`0.055` with increment `0.01`).
- a non-default increment (e.g. `0.02`) produces correctly stepped values, and bounds misaligned to that increment are rejected.
- scan values are produced in ascending order.

### Mapper Tests

`AnalysisConfigMapperTest`

- maps DBSCAN epsilon range.
- maps hierarchical threshold range.
- maps `scanIncrement`, and defaults it to `0.01` when absent.
- rejects missing `from`.
- rejects missing `to`.
- rejects an old-style scalar `epsilon`/`threshold` with a clear, parameter-named error message.

### Analyzer Tests

`AnalyzerTest`

- with hierarchical range `0.03..0.05`, result contains three scan entries.
- each scan entry has `parameter = "threshold"` and values `0.03`, `0.04`, `0.05` in ascending order.
- syntactic analysis is calculated separately per scan, based on that scan's clusters.
- literal analysis is present once at top level.
- DBSCAN range similarly produces one scan per epsilon, and the fixed `minPts` is recorded in the output config.

### Golden JSON Tests

Update the existing expected analysis JSON fixture:

- replace top-level `semantic` and `syntactic` with `scans`
- assert `literal` remains top-level
- assert `scans` is ascending by `value`
- `value` is serialized as a JSON number. Tests MUST compare it numerically (with a small tolerance), not as an exact string. JSON has no decimal-places semantics, so `0.06` vs `0.060` vs a float-formatted variant are not string-stable across serializer versions. The "two decimal places" requirement is a display/formatting concern at the serialization boundary, not a JSON-level guarantee, and tests must not assert it via string equality.

## Documentation Updates

Update:

- `README.md` analysis section
- `specs/yaml-analysis-config-plan-pairing.md`
- any sample plans under `src/main/resources/plans` and `src/test/resources/plans`

The documentation should make clear that the scan is intended to make cluster stability visible across nearby thresholds, not to automatically choose the "best" threshold, and that interpreting the scan (plateau detection, representative-value selection) is a separate downstream step outside this spec.
