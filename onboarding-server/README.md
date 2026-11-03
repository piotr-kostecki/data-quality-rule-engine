# onboarding-server

The runnable **Spring Boot** application. It holds the **adapters** around
[`onboarding-application`](../onboarding-application) (REST in; persistence and events out) and the
wiring that connects them. Business logic lives in the inner modules, never here.

[← Back to the main README](../README.md)

## Run

```bash
./gradlew :onboarding-server:bootRun                                          # inside the Nix shell (dev profile)
./gradlew :onboarding-server:bootRun --args='--spring.profiles.active=dev'    # outside it
./gradlew :onboarding-server:bootJar && java -jar onboarding-server/build/libs/onboarding-server-*-SNAPSHOT.jar
```

| URL | What |
|---|---|
| `http://localhost:8080/swagger-ui.html` | OpenAPI UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI document |
| `http://localhost:8080/actuator/health` | health |

Ready-made requests are in [`http/requests.http`](../http/requests.http).

## Contents

| Package | Role | Types |
|---|---|---|
| `adapter.in.web` | driving adapter: REST API, DTOs, RFC 9457 error responses | `BusinessPartnerController`, `ApiProblems`, `dto.*` |
| `adapter.out.persistence` | driven adapter for `LoadPartnerPort` + `SavePartnerPort` | `InMemoryPartnerRepository` |
| `adapter.out.event` | driven adapter for `PublishPartnerEventPort`, and an example consumer of the event | `SpringEventPublisherAdapter`, `OnboardingAuditListener` |
| `config` | composition root, configuration properties, demo data | `OnboardingConfiguration`, `MatchingProperties`, `DemoDataLoader` |

## API

| Method and path | Outcome |
|---|---|
| `POST /api/v1/business-partners` | `201` + `Location` · `422` rules failed · `409` duplicate · `400` malformed JSON |
| `POST /api/v1/business-partners/checks` | `200` with the analysis; nothing is stored |
| `GET /api/v1/business-partners/{id}` | `200` · `404` unknown id · `400` malformed id |
| `GET /api/v1/business-partners?country=DE` | `200` list, country is case-insensitive |

Every error is `application/problem+json` (RFC 9457).

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `onboarding.matching.candidate-threshold` | `0.80` | score from which a partner is reported as a possible duplicate |
| `onboarding.matching.blocking-threshold` | `0.95` | score from which onboarding is refused (`409`) |

Invalid thresholds (outside 0..1, or candidate above blocking) stop the application at start-up.

| Profile | Effect |
|---|---|
| `dev` | set by the Nix shell; includes `demo`, DEBUG logs, indented JSON, all actuator endpoints |
| `demo` | seeds three fictional partners (DE, PL, CH) |
| `json-logs` | logs as JSON in the Logstash event format |

## Logging

The code uses the SLF4J API. The backend is Log4j2 (`spring-boot-starter-log4j2`), and Logback is
excluded. The output is defined in
[`src/main/resources/log4j2-spring.xml`](src/main/resources/log4j2-spring.xml): readable lines by
default, and `LogstashJsonEventLayoutV1` JSON on stdout with the `json-logs` profile. Levels are set in
`application*.yml`. See [ADR-0009](../docs/adr/0009-logging-slf4j-log4j2.md).

## Dependencies

`onboarding-application` (and through it `partner-quality`), plus the Spring Boot web and actuator
starters, springdoc-openapi and Log4j2. Lombok (`@RequiredArgsConstructor`, `@Slf4j`) is used at compile
time only ([ADR-0010](../docs/adr/0010-lombok.md)). Spring versions come from the Spring Boot BOM, which is imported
with Gradle's `platform()`. All other versions come from
[`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

## Tests

```bash
./gradlew :onboarding-server:test
```

| Test | Level | What it proves |
|---|---|---|
| `BusinessPartnerControllerTest` | web slice, incoming ports mocked | HTTP status, headers and body for every outcome |
| `OnboardingApiIntegrationTest` | full context | the whole flow end to end, OpenAPI and health endpoints |
| `MatchingThresholdsConfigurationTest` | full context | thresholds come from configuration, and invalid values fail fast |
| `DemoProfileTest` | full context, `demo` profile | the seed data the README examples rely on |
| `HexagonalArchitectureTest` | ArchUnit | onion layering, adapters use ports only, no use of the library's `internal` packages |

The `test` task removes `SPRING_PROFILES_ACTIVE`, so tests never inherit the shell's `dev` profile.
Reports are in `build/reports/tests/test/index.html`.
