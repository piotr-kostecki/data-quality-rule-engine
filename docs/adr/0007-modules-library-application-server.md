# ADR-0007: Three Gradle modules: library, application, server

- **Status:** accepted
- **Date:** 2026-10-26

## Context

Data quality rules and duplicate detection are valuable beyond this one service: a batch cleansing job,
an ERP integration or another team's service could use them. In a single module nothing stops Spring or
the onboarding use cases from leaking into that logic, and nothing marks which classes are a stable API.

## Decision

| Module | Contents | Allowed dependencies |
|---|---|---|
| `partner-quality` | Published **library**: `PartnerData`, rules, engine, duplicate detector | JSpecify only |
| `onboarding-application` | The **hexagon**: entity, ports, use-case service | `partner-quality` |
| `onboarding-server` | **Adapters** + Spring Boot wiring; the deployable | `onboarding-application` (+ Spring) |

Library specifics (details in [LIBRARY_GUIDELINES.md](../LIBRARY_GUIDELINES.md)):

- JPMS `module-info.java` exports `com.example.partnerquality`, `.validation` and `.matching`. The
  `.internal` packages are hidden.
- Built-in rules are reached only through the `BuiltInRules` factory, so implementations stay private.
- `DuplicateDetector` is generic (`Match<T>`), so it returns the application's own `BusinessPartner`
  without knowing that type.
- Stricter build (`-Werror`, doclint), published with sources, javadoc and test fixtures (`PartnerFixtures`).

The library's `PartnerData` doubles as the application's input model. That avoids a duplicate
`PartnerDraft` type and a mapping layer; `BusinessPartner` wraps it and adds identity and a timestamp.

## Consequences

- ➕ Dependency direction is enforced by the compiler, not by discipline.
- ➕ The library can be versioned and released independently; consumers get a small, documented API.
- ➕ Builds parallelise and cache per module; changing the server doesn't recompile or retest the library.
- ➖ The application's ports expose library types (`PartnerData`, `Match`, `ValidationReport`), which ties the
  application to the library's API. That's acceptable, because the library *is* the domain. If it were a
  generic utility, the application would wrap those types instead.
- ➖ More build files and conventions to understand (mitigated by ADR-0008).
