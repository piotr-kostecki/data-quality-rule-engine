# ADR-0002: Data quality rules as independent strategies

- **Status:** accepted
- **Date:** 2026-10-25

## Context

Rules vary by country, have different severities, and new ones are added over time. A client (a UI or
an ERP integration) needs **all** problems in one response to show
them together, not one at a time.

## Decision

- Each rule implements `DataQualityRule` (`id()`, `check(PartnerData) → List<RuleViolation>`). This is
  the Strategy pattern. It is the library's public extension point; built-in implementations are internal
  and only reachable through the `BuiltInRules` factory.
- `DataQualityEngine` runs all rules and collects every violation; it does not stop at the first one.
  A rule that throws becomes a `RULE_FAILURE` error (fail closed) instead of aborting validation.
- Two severities: `ERROR` blocks onboarding; `WARNING` is reported but allowed (for example a missing VAT
  ID, or an IBAN from another country).
- Rules degrade gracefully: an unknown country means "no format check" (no violation, or a warning),
  never a false error.
- `ruleId` and `field` are stable, machine-readable parts of the API contract. `message` is for humans.
- Input hygiene (trim, blank → null, compact identifiers) happens once, in the `PartnerData` constructor,
  so individual rules only need null checks.

## Consequences

- ➕ Adding a rule = one class + one line of wiring + its tests (Open/Closed principle).
- ➕ Every rule can be tested in complete isolation.
- ➖ Rules cannot depend on each other's results. Where it matters, a rule re-checks cheap preconditions
  itself (`VatIdRule` only compares the prefix when the country is valid).
- ➖ Thousands of rules will not be Java classes. The next step is a data-driven `ConfigurableRule`
  (regex or value lists per country, stored and versioned), keeping Java for algorithms such as checksums.

## Alternatives considered

- **Bean Validation annotations.** Cannot express severities or country-specific checksums cleanly, and is
  tied to the framework.
- **A rules engine (Drools, Easy Rules).** Powerful, but a heavy dependency and a new language for the
  team; premature at this size.
