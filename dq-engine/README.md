# dq-engine

The core of the data-quality rule engine. It runs a catalog of rules over a stream of records and emits one
explained **result** per applicable rule and record. Rule failures become isolated **errors**, and the run ends
with a **summary**. Plain Java 21. The only dependency is JSpecify (null-safety annotations), and the module
has no logging and no I/O.

[← Back to the main README](../README.md) · Design: [DESIGN.md](../DESIGN.md) · Rule logic in CEL: [`dq-engine-cel`](../dq-engine-cel)

## Concepts

| Type | What |
|---|---|
| `Rule` | id, label, `RuleLogic`, `DecisionMapping`, `RuleStatus`, `Severity`, `CountryScope`, categories |
| `RuleLogic` | computes a **value** (e.g. `"ok"`, `"blocked"`) from a record. It never returns a decision. The seam for logic: CEL in `dq-engine-cel`, or plain Java via `RuleLogic.of(...)` |
| `DecisionMapping` | value → `Decision` as data, with a default. If no default is given, the default is `REVIEW` (assumption A-1) |
| `DataRecord` | one input record: field name → value (`String`, `Long`, `Double`, `Boolean`, `List`, `Map`) |
| `RecordSchema` | which fields hold the record id and country (default `id`, `country`) |
| `Result` / `RuleError` / `RuleSkipped` | the sealed `Outcome`s. Results and errors carry the record, the rule and the fields read with their values |
| `RunSummary` | records, rules selected, results, errors, skips, results by decision and by severity |

## Usage

```java
ValidationEngine engine = ValidationEngine.builder()
        .catalog(RuleCatalog.of(rules))          // duplicate rule ids fail here
        .build();                                // immutable, thread-safe: build once, share

// Large input: stream, never hold all results
RunSummary summary = engine.run(records, new ResultSink() {
    public void onResult(Result result) { /* write it somewhere */ }
    public void onError(RuleError error) { /* write it somewhere */ }
    public void onBatchEnd(BatchInfo batch) { /* flush, commit, checkpoint */ }
}, RunOptions.DEFAULT
        .withFilter(RuleFilter.category("tax").and(RuleFilter.country("DE")))
        .withBatchSize(1_000));

// Small input (one HTTP request): collect everything
ValidationReport report = engine.validate(List.of(record1, record2));
```

## Behaviour

- **Selection:** the caller's `RuleFilter` (status, category, country, composed with `and`/`or`/`negate`)
  is always combined with `status == RELEASED`.
- **Applicability:** a rule runs on a record only if its scope is world or contains the record's country.
  Rules are indexed by country once per run. A record with no country, or an unknown one, gets world rules
  only (assumption A-3). Skipped pairs are counted, and at `ExplainLevel.SKIPS` they are also emitted as
  `RuleSkipped` events, never as results (assumption A-2).
- **Fault isolation:** each rule × record evaluation is isolated.
  - A `MissingFieldException` becomes `MISSING_FIELD`.
  - Any other exception, or a `null` value, becomes `EVALUATION_FAILED`.
  - A record whose country is not text becomes `BAD_RECORD`, and no rule runs on it.

  In each case the run continues. Only failures in host code (the sink or the record source) stop it, with
  `RunAbortedException`, which carries the summary so far.
- **Bounded memory:** records are pulled lazily in batches of `batchSize`, and outcomes are pushed to the sink
  immediately. Summary counters are fixed-size.

Assumptions marked `ASSUMPTION(A-n)` in the code are listed in DESIGN.md §10 and need confirmation.

## API guarantees

- JPMS module `com.example.dq.engine`, exporting the single package `com.example.dq.engine`.
- Everything is non-null unless annotated `@Nullable` (`@NullMarked`).
- All public types are immutable and thread-safe, except the `ResultSink` the host provides.
- No threads are started, and runs execute on the caller's thread.

## Tests

```bash
./gradlew :dq-engine:test
```

| Test | What it proves |
|---|---|
| `ValidationEngineTest` | results with provenance, the `RELEASED` guard, filters, country scope, skips (A-2), unknown country (A-3), every isolated error kind, summary counts, batching order, sink failure aborts |
| `DecisionMappingTest` | explicit case, default, implicit `REVIEW` default (A-1), duplicate values rejected |
| `RuleFilterTest` | status, category and country filters, and their composition |
| `BoundedMemoryTest` | 1,000,000 records × 3 rules stream through on a **64 MB** test heap. Collecting the same run in memory fails with `OutOfMemoryError` |

Rules in these tests use plain-Java logic, so the engine is tested without an expression language.
Reports are in `build/reports/tests/test/index.html`.
