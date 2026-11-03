# ADR-0001: Hexagonal architecture with explicit ports and adapters

- **Status:** accepted (revised: ports and adapters now have dedicated packages and modules)
- **Date:** 2026-10-26

## Context

The value of this service is its business logic: data quality rules and duplicate detection. The
technology around it will change. Storage starts in memory but would move to a database, and events
start in-process but would move to a message broker. The logic must be testable quickly and in isolation. A reviewer
must be able to **see** the ports and adapters, not infer them from naming.

## Decision

Ports and adapters (Alistair Cockburn), with each role in its own package:

| Role | Package | Module |
|---|---|---|
| Domain model and services | `com.example.partnerquality..` (library), `..application.domain` (entity, event) | `partner-quality`, `onboarding-application` |
| **Incoming (driving) ports** | `..application.port.in`: `OnboardPartnerUseCase`, `CheckPartnerUseCase`, `FindPartnersQuery` | `onboarding-application` |
| **Outgoing (driven) ports** | `..application.port.out`: `LoadPartnerPort`, `SavePartnerPort`, `PublishPartnerEventPort` | `onboarding-application` |
| Use-case implementation | `..application.service.OnboardingService` (implements in-ports, uses out-ports) | `onboarding-application` |
| **Driving adapters** | `..server.adapter.in.web`: REST controller, DTOs, problem details | `onboarding-server` |
| **Driven adapters** | `..server.adapter.out.persistence`, `..server.adapter.out.event` | `onboarding-server` |
| Composition root | `..server.config`: the only code that knows `OnboardingService` | `onboarding-server` |

Rules:

- Ports are **small and role-based** (interface segregation): one per capability, not one "repository" per
  aggregate. The in-memory adapter happens to implement two of them; a future read model could implement
  only `LoadPartnerPort`.
- Driving adapters depend on **in-ports only**. The controller never sees `OnboardingService` or any out-port.
- Use cases take **command objects** (`OnboardPartnerCommand`) so they can evolve without signature changes.
- `domain`, `port` and `service` contain no Spring. Wiring happens with `@Bean` methods in `OnboardingConfiguration`.

Enforcement: Gradle module dependencies (compile time) plus `HexagonalArchitectureTest` (ArchUnit:
onion architecture, adapters → ports only, `adapter.in` ↛ `port.out`, no use of library internals).

## Consequences

- ➕ The architecture is visible in the folder tree; a newcomer finds "where does HTTP go in" and "where
  does storage go out" without reading code.
- ➕ Use-case tests need no mocking framework: `InMemoryPartnerStore` is a ~50-line fake of all out-ports.
- ➕ Replacing storage or messaging = one new adapter class; the hexagon is untouched.
- ➖ More types (three in-ports, three out-ports, a command) than a "controller → service → repository"
  layering. For CRUD this is overhead; here the logic justifies it.
- ➖ Manual wiring in a configuration class instead of `@Service` scanning.

## Alternatives considered

- **Classic layered architecture with Spring annotations everywhere.** Less code, but the domain becomes
  coupled to the framework and the boundaries are only conventions.
- **One port per aggregate** (`BusinessPartnerRepository`). Fewer types, but every adapter and fake has to
  implement every method, even ones it doesn't need.
- **Spring Modulith.** Good for module boundaries inside one deployable; overlaps with Gradle modules here.
