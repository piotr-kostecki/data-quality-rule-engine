# onboarding-application

The **hexagon** of the onboarding service: domain model, incoming and outgoing **ports**, and the
use-case service that implements them. Plain Java 21 with no framework. It doesn't know about HTTP,
Spring, storage or messaging.

[← Back to the main README](../README.md)

## Contents

| Package | What | Types |
|---|---|---|
| `domain` | the onboarded partner and its event | `BusinessPartner`, `PartnerId`, `BusinessPartnerOnboarded` |
| `port.in` | what the outside world may ask (implemented here, called by driving adapters) | `OnboardPartnerUseCase`, `CheckPartnerUseCase`, `FindPartnersQuery`, `OnboardPartnerCommand`, `OnboardingResult`, `QualityCheck` |
| `port.out` | what the application needs from the outside world (implemented by driven adapters) | `LoadPartnerPort`, `SavePartnerPort`, `PublishPartnerEventPort` |
| `service` | orchestration of the use cases | `OnboardingService` |

`OnboardingService.onboard(...)` takes three steps:
1. Validate with the `DataQualityEngine`. Any `ERROR` gives `Rejected`.
2. Look for duplicates among the partners with the same country or IBAN (`DuplicateDetector`). A
   blocking match gives `DuplicateSuspected`.
3. Otherwise save the partner, publish `BusinessPartnerOnboarded`, and return `Onboarded` with the
   warnings and any weaker candidates.

`check(...)` runs steps 1 and 2 only and stores nothing (a dry run). `OnboardingResult` is a sealed
interface, so callers handle every outcome with an exhaustive `switch`.

## Dependencies

| Dependency | Scope | Why |
|---|---|---|
| [`partner-quality`](../partner-quality) | `api` | its types (`PartnerData`, `ValidationReport`, `Match`) appear in this module's port signatures |
| `partner-quality` test fixtures | test | valid sample partners |

There are no other runtime dependencies and no logging. The adapters in
[`onboarding-server`](../onboarding-server) implement the outgoing ports and call the incoming ones.

## Rules for changes

- Nothing here may import Spring, a database or a message broker. The Gradle module boundary makes that
  a compile error, and `HexagonalArchitectureTest` in `onboarding-server` checks the package rules.
- New infrastructure needs a new **outgoing port** here, plus an adapter in `onboarding-server`.
- New behaviour for callers needs a new or extended **incoming port**.

## Tests

```bash
./gradlew :onboarding-application:test
```

`OnboardingServiceTest` runs the use cases with the real library and `InMemoryPartnerStore`. That fake
implements all outgoing ports and records published events, so no mocks are needed. It covers onboarding,
rejection, duplicates (same VAT ID, same IBAN in another country), the dry run with and without a blocking
duplicate, and lookup by country. Reports are in `build/reports/tests/test/index.html`.
