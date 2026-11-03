# dq-engine-cel

Rule logic for [`dq-engine`](../dq-engine) written as **CEL** (Common Expression Language,
[cel.dev](https://cel.dev)) expressions. A rule's logic becomes a string, which is data that can live in a
catalog, while the engine stays free of dependencies.

[← Back to the main README](../README.md) · Why CEL: [DESIGN.md §3](../DESIGN.md#rule-logic-cel-expressions)

## Usage

```java
CelLogicCompiler cel = CelLogicCompiler.create();      // immutable, thread-safe: create once

Rule ibanFormat = Rule.builder("ibanFormat")
        .status(RuleStatus.RELEASED)
        .logic(cel.compile("present(record, 'iban') && record.iban.startsWith(record.country) ? 'ok' : 'bad'"))
        .mapping(DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault())
        .build();
```

## The expression language

| Element | Meaning |
|---|---|
| `record` | the only variable: the record's fields, `map(string, dyn)` |
| `record.x`, `record['x']` | a field's value. **Fails** with `MISSING_FIELD` if the field is absent |
| `has(record.x)` | the field exists |
| `present(record, 'x')` | the field exists, is a string and is not blank (provided by this module) |
| standard CEL | `==`, `<`, `&&`, `\|\|`, `!`, `? :`, `size`, `startsWith`, `endsWith`, `contains`, `matches`, `int()`, `string()`, macros `all`, `exists`, `map`, `filter` |
| result | must be a **string**: the rule's computed value. A `bool` result is rejected, because the decision belongs in the mapping |

What happens at each stage:
- **Compile time** (`compile`): the expression is parsed and type-checked. A syntax error, an unknown function
  or variable, or a non-string result throws `RuleCompilationException`, before any record is validated.
- **Evaluation:** an absent field read without a guard throws `MissingFieldException`, which the engine reports
  as `MISSING_FIELD`. Any other failure, such as a wrong value type, throws `ExpressionEvaluationException`,
  reported as `EVALUATION_FAILED`.
- **Provenance:** the fields an expression references (`record.x`, `record['x']`, `has(record.x)`,
  `present(record, 'x')`) are collected from the syntax tree once. Every result lists them with their values.

CEL has no side effects and always terminates, so a rule can't do I/O or loop forever.

## Dependencies

- `dq-engine` (`api`).
- `dev.cel:cel` (`implementation`), which brings Guava and protobuf. It is kept out of `dq-engine` for this
  reason.
- Version **0.14.0**: 0.15.0 was published only a week before this was written, so the previous release is
  used.
- The CEL jars declare no Java module names, so this module is a classpath library without a
  `module-info`.

Jackson is a test-only dependency, used to read the task's JSON fixture. The library itself doesn't parse JSON.

## Tests

```bash
./gradlew :dq-engine-cel:test
```

| Test | What it proves |
|---|---|
| `RequirementScenarioTest` | the task's scenario: the 3 records from `src/test/resources/fixtures/records.json` × the 3 rules in CEL give exactly the expected decision table, with provenance and summary counts |
| `FaultIsolationTest` | a rule that fails on one record (`MISSING_FIELD`) and a broken record (`BAD_RECORD`): the rest of the batch completes and both failures are reported with their cause |
| `CelLogicCompilerTest` | evaluation, JSON numbers and booleans, `present()`, referenced fields, missing-field and type errors, compile-time rejection of invalid expressions |

The scenario's rules are in `FixtureRules` and its records in `FixtureRecords`.
Reports are in `build/reports/tests/test/index.html`.
