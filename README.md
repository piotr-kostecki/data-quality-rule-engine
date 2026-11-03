# Business Partner Onboarding Service

A service that checks new customers and suppliers **before** they enter the master data: data quality
rules (VAT ID, IBAN, postal codes, mandatory fields) and duplicate detection (fuzzy name matching plus
identifiers).

Requirements for the data-quality rule engine are in [raw/REQUIREMENT.md](raw/REQUIREMENT.md), and
its design is in [DESIGN.md](DESIGN.md).

**Stack:** Java 21 · Spring Boot 3.5 · Gradle 8.14 (Groovy DSL, version catalog, convention plugins) ·
JUnit 5 · AssertJ · Mockito · ArchUnit · JSpecify · springdoc-openapi · SLF4J + Log4j2 · Nix flake + direnv.

## Three modules

```
┌──────────────────────────── onboarding-server (Spring Boot) ─────────────────────────────┐
│  adapter/in/web                      config                     adapter/out/persistence  │
│  BusinessPartnerController ──┐   OnboardingConfiguration    ┌── InMemoryPartnerRepository│
│  (driving adapter)           │   (composition root)         │   adapter/out/event        │
│                              │                              ├── SpringEventPublisherAdapter
│   ┌──────────────────────────▼─── onboarding-application ───┴──────────────────────────┐ │
│   │  port/in   OnboardPartnerUseCase · CheckPartnerUseCase · FindPartnersQuery         │ │
│   │  service   OnboardingService  implements port/in, uses port/out                    │ │
│   │  port/out  LoadPartnerPort · SavePartnerPort · PublishPartnerEventPort             │ │
│   │  domain    BusinessPartner · PartnerId · BusinessPartnerOnboarded                  │ │
│   │                         │ uses                                                     │ │
│   │   ┌─────────────────────▼────── partner-quality (library) ───────────────────┐     │ │
│   │   │ validation  DataQualityEngine · DataQualityRule · BuiltInRules            │     │ │
│   │   │ matching    DuplicateDetector · Match<T> · MatchingPolicy                 │     │ │
│   │   │ (root)      PartnerData · Address        internal/ (hidden by module-info)│     │ │
│   │   └───────────────────────────────────────────────────────────────────────────┘     │ │
│   └─────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

| Module | What | Depends on | Framework? | Published? |
|---|---|---|---|---|
| [`partner-quality`](partner-quality) | **Library**: rules, rule engine, duplicate detection | JSpecify only | no | yes: jar, sources, javadoc, test fixtures |
| `onboarding-application` | **Hexagon**: domain entity, incoming/outgoing **ports**, use-case service | `partner-quality` | no | no |
| `onboarding-server` | **Adapters** (REST in; persistence and events out) and Spring wiring | `onboarding-application` | Spring Boot | runnable jar |

Dependencies only point inward, and two mechanisms enforce that:

1. **Gradle module boundaries**: `partner-quality` can't see the application, and the application can't
   see Spring. Breaking either is a compile error.
2. **`HexagonalArchitectureTest` (ArchUnit)** guards what Gradle can't:
   - adapters don't depend on each other;
   - adapters use ports, never the service;
   - driving adapters never touch outgoing ports;
   - nobody imports the library's `internal` packages.

### Ports and adapters at a glance

| Kind | Port (interface, in `onboarding-application`) | Adapter (in `onboarding-server`) |
|---|---|---|
| Driving / in | `port.in.OnboardPartnerUseCase`, `CheckPartnerUseCase`, `FindPartnersQuery` | `adapter.in.web.BusinessPartnerController` |
| Driven / out | `port.out.LoadPartnerPort`, `SavePartnerPort` | `adapter.out.persistence.InMemoryPartnerRepository` |
| Driven / out | `port.out.PublishPartnerEventPort` | `adapter.out.event.SpringEventPublisherAdapter` |

`OnboardingService` implements the in-ports and is created only in `config.OnboardingConfiguration`. The
controller receives it through the port interfaces and never sees the class.

## Quick start

**Why Nix:** the development environment is defined in a Nix flake for two reasons.
- **Reproducibility:** `flake.lock` pins the exact JDK 21, Gradle and tool versions, so every machine
  and CI uses the same toolchain.
- **Easy setup:** one command (`direnv allow` or `nix develop`) gives a working shell. You don't install
  a JDK or Gradle by hand, and nothing is installed globally.

See [ADR-0006](docs/adr/0006-nix-dev-environment.md).

### With Nix + direnv (recommended, nothing else needed)

```bash
direnv allow                 # first time only; loads JDK 21 + Gradle from flake.nix
./gradlew build              # compiles, tests (92) and packages all modules
./gradlew bootRun            # runs onboarding-server with the dev profile (set by the shell)
./gradlew :partner-quality:publishToMavenLocal   # installs the library into ~/.m2
```

Without direnv: `nix develop`. Details in [docs/DEV_ENVIRONMENT.md](docs/DEV_ENVIRONMENT.md).

### Without Nix

You need a JDK between 17 and 24 on `PATH` to launch Gradle 8.14, which cannot run on JDK 25. The wrapper
downloads Gradle, and the foojay toolchain resolver downloads JDK 21 for compiling if it is missing:

```bash
./gradlew build
./gradlew :onboarding-server:bootRun --args='--spring.profiles.active=dev'
```

### Editor and formatter config (optional)

I work in Neovim, so the repo contains a few formatter and editor files. They are optional: if you use
another editor, or don't need them, ignore them. The build doesn't use them.

| File | Purpose |
|---|---|
| `.lazy.lua` | Neovim (LazyVim) project file: Java formatter, import order, null analysis, Gradle import with JDK 21 |
| `.editorconfig` | basic formatting for every editor: UTF-8, LF, 4 spaces, 120 columns |
| `config/eclipse-formatter.xml` | Java formatter profile, used by jdtls and importable into IntelliJ or Eclipse |

Details in [docs/DEV_ENVIRONMENT.md](docs/DEV_ENVIRONMENT.md#editor-setup-and-formatting).

### Spring profiles

| Profile | Activated by | What it does |
|---|---|---|
| *(none)* | default outside the Nix shell | empty in-memory store, INFO logs, actuator `health,info,metrics` |
| `dev` | `SPRING_PROFILES_ACTIVE=dev`, exported by the Nix shell | includes `demo`; DEBUG logs; indented JSON; all actuator endpoints; health details |
| `demo` | `dev`, or on its own | seeds three fictional partners (DE, PL, CH) |
| `json-logs` | added explicitly, e.g. `SPRING_PROFILES_ACTIVE=dev,json-logs` | logs as one JSON object per line in the Logstash event format (see below) |

Tests never inherit the shell's profile: the server's `test` task removes `SPRING_PROFILES_ACTIVE`.

### Logging

Code logs through the SLF4J API, and Log4j2 is the backend. The output is configured in
[`log4j2-spring.xml`](onboarding-server/src/main/resources/log4j2-spring.xml), and levels in `application*.yml`. Logs always
go to stdout. By default they are human-readable lines. With the `json-logs` profile they are JSON, ready
for a log shipper or Logstash:

```json
{"@version":1,"@timestamp":"2026-11-03T05:22:52.739+0100","level":"INFO","service":"partner-onboarding",
 "logger_name":"com.example.onboarding.server.adapter.out.event.OnboardingAuditListener","thread_name":"main",
 "message":"Business partner onboarded: id=27d8f152-… name='Acme Chemicals GmbH' country=DE"}
```

See [ADR-0009](docs/adr/0009-logging-slf4j-log4j2.md).

### Where to look once it runs

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check |

[http/requests.http](http/requests.http) has ready-made requests for IntelliJ, or for VS Code with the REST Client extension.

## Build setup (Groovy DSL)

```
settings.gradle                  modules, repositories (centrally), foojay toolchains, includeBuild('build-logic')
gradle.properties                group, version, build cache, parallel, configuration cache
gradle/libs.versions.toml        version catalog: every external version in one place
build-logic/                     included build with Groovy convention plugins
  src/main/groovy/onboarding.java-conventions.gradle      toolchain 21, -Xlint, JUnit 5 + AssertJ
  src/main/groovy/onboarding.library-conventions.gradle   + java-library, -Werror, javadoc/sources jars,
                                                           reproducible jars, maven-publish
partner-quality/build.gradle          id 'onboarding.library-conventions' + java-test-fixtures
onboarding-application/build.gradle   id 'onboarding.java-conventions' + java-library
onboarding-server/build.gradle        id 'onboarding.java-conventions' + Spring Boot (BOM via platform())
```

Module build files only declare what's specific to them; `partner-quality/build.gradle` is a handful of lines. To add a dependency, add it to `libs.versions.toml` and reference it as `libs.<name>`.

## API

| Method & path | Purpose | Responses |
|---|---|---|
| `POST /api/v1/business-partners/checks` | Dry run: violations + duplicate candidates, stores nothing | `200` |
| `POST /api/v1/business-partners` | Onboard a partner | `201` created · `422` rule errors · `409` duplicate · `400` bad JSON |
| `GET /api/v1/business-partners/{id}` | Fetch one | `200` · `404` · `400` malformed id |
| `GET /api/v1/business-partners?country=PL` | List (optionally by country) | `200` |

All errors use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`).

### Examples (real output from the dev profile)

A spelling variant of an existing partner is recognised as a duplicate:

```bash
curl -s -X POST localhost:8080/api/v1/business-partners/checks -H 'Content-Type: application/json' -d '{
  "name": "ACME Chemicals G.m.b.H.",
  "address": {"street": "Industriestr. 1", "postalCode": "10115", "city": "Berlin", "country": "DE"}}'
```

```json
{
  "onboardable": false,
  "violations": [
    { "ruleId": "VAT_ID", "field": "vatId", "severity": "WARNING",
      "message": "VAT ID is missing; duplicate detection will rely on name and address only" }
  ],
  "duplicates": [
    { "partnerId": "772a87af-…", "name": "Acme Chemicals GmbH", "score": 1.0, "reason": "SIMILAR_NAME_AND_LOCATION" }
  ]
}
```

All problems are reported at once, with errors and warnings kept apart:

```bash
curl -s -X POST localhost:8080/api/v1/business-partners -H 'Content-Type: application/json' -d '{
  "name": "Broken Corp", "address": {"city": "Berlin", "postalCode": "1234", "country": "DE"},
  "vatId": "DE136695977", "iban": "CH9300762011623852957"}'
```

```json
{
  "type": "urn:problem:partner-onboarding:validation-failed",
  "title": "Data quality check failed",
  "status": 422,
  "detail": "2 data quality rule(s) failed",
  "instance": "/api/v1/business-partners",
  "violations": [
    { "ruleId": "MANDATORY_FIELDS",   "field": "address.street",     "severity": "WARNING", "message": "Street is missing" },
    { "ruleId": "POSTAL_CODE_FORMAT", "field": "address.postalCode", "severity": "ERROR",   "message": "'1234' is not a valid postal code for DE" },
    { "ruleId": "VAT_ID",             "field": "vatId",              "severity": "ERROR",   "message": "VAT ID 'DE136695977' has an invalid check digit" },
    { "ruleId": "IBAN",               "field": "iban",               "severity": "WARNING", "message": "IBAN country CH differs from address country DE" }
  ]
}
```

## Tests

| Module | Level | Classes | Spring? | What it proves |
|---|---|---|---|---|
| partner-quality | unit, via public API | `VatIdRuleTest`, `IbanRuleTest`, `AddressRulesTest`, `DataQualityEngineTest`, `DuplicateDetectorTest`, `MatchingPolicyTest`, `PartnerDataTest` | no | rules, engine fault isolation, matching and its thresholds, input normalization (reference vectors) |
| partner-quality | unit, whitebox | `JaroWinklerTest`, `NameNormalizerTest` | no | internal algorithms |
| onboarding-application | use case | `OnboardingServiceTest` with the `InMemoryPartnerStore` fake | no | orchestration through ports; no mocks needed |
| onboarding-server | web slice | `BusinessPartnerControllerTest` (in-ports mocked) | MVC only | HTTP contract per outcome |
| onboarding-server | integration | `OnboardingApiIntegrationTest` | full | the whole flow, OpenAPI, actuator |
| onboarding-server | integration | `MatchingThresholdsConfigurationTest` | full | duplicate thresholds come from `onboarding.matching.*`; invalid values stop the start-up |
| onboarding-server | integration | `DemoProfileTest` | full | the `demo` seed data that the README examples and `http/requests.http` rely on |
| onboarding-server | architecture | `HexagonalArchitectureTest` | no | onion layering and port rules |

Run them with `./gradlew test`. Each module prints a summary line, for example
`partner-quality: 67 tests, 67 passed, 0 failed, 0 skipped -> SUCCESS`; failures are printed in full.

Results per module:

| What | Where |
|---|---|
| HTML report (open in a browser) | `<module>/build/reports/tests/test/index.html` |
| JUnit XML (for CI and tools) | `<module>/build/test-results/test/*.xml` |

`./gradlew test` skips modules whose code hasn't changed (up to date). Use `./gradlew test --rerun-tasks` to
run everything again.

## Decisions

| ADR | Decision |
|---|---|
| [0001](docs/adr/0001-hexagonal-architecture.md) | Hexagonal architecture: explicit ports and adapters, framework-free core |
| [0002](docs/adr/0002-pluggable-rule-engine.md) | Data quality rules as independent strategies; ERROR vs WARNING; report all findings |
| [0003](docs/adr/0003-duplicate-detection.md) | Duplicate detection: normalization, Jaro-Winkler, identifier evidence, two thresholds |
| [0004](docs/adr/0004-in-memory-store-and-in-process-events.md) | In-memory store and in-process events now; MongoDB + outbox + Kafka later |
| [0005](docs/adr/0005-error-model.md) | Business outcomes as a sealed result type; errors as RFC 9457 problems |
| [0006](docs/adr/0006-nix-dev-environment.md) | Reproducible toolchain with a Nix flake and direnv |
| [0007](docs/adr/0007-modules-library-application-server.md) | Three Gradle modules: library, application (hexagon), server (adapters) |
| [0008](docs/adr/0008-gradle-groovy-version-catalog.md) | Groovy DSL, version catalog, convention plugins in build-logic |
| [0009](docs/adr/0009-logging-slf4j-log4j2.md) | SLF4J API with Log4j2; JSON (Logstash format) output via a profile |

## Further documentation

- **[docs/LIBRARY_GUIDELINES.md](docs/LIBRARY_GUIDELINES.md)**: how to build a scalable, durable Java
  library, what `partner-quality` already does and what to add next.
- **[partner-quality/README.md](partner-quality/README.md)**: the library from a consumer's point of view.
