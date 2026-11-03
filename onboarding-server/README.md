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
| `http://localhost:8080/actuator/health` | health; `/liveness` and `/readiness` for Kubernetes probes |
| `http://localhost:8080/actuator/info` | build (artifact, version), Java and OS |
| `http://localhost:8080/actuator/prometheus` | metrics for Prometheus to scrape |

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
| `otel` | exports traces and metrics over OTLP/HTTP to `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://localhost:4318`) |

## Logging

The code uses the SLF4J API. The backend is Log4j2 (`spring-boot-starter-log4j2`), and Logback is
excluded. The output is defined in
[`src/main/resources/log4j2-spring.xml`](src/main/resources/log4j2-spring.xml): readable lines by
default, and `LogstashJsonEventLayoutV1` JSON on stdout with the `json-logs` profile. Levels are set in
`application*.yml`. See [ADR-0009](../docs/adr/0009-logging-slf4j-log4j2.md).

## Observability

- **Tracing:** Micrometer Observation with the OpenTelemetry bridge. Every HTTP request is a span, and an
  incoming W3C `traceparent` is continued. Sampling is 10% of new traces by default and 100% with `dev`
  or `otel`.
- **Log correlation:** while a request is traced, each log line shows `[traceId,spanId]`. In JSON logs both
  are in the `mdc` object.
- **Export:** with the `otel` profile, traces and metrics are pushed with OTLP/HTTP to an OpenTelemetry
  Collector or any OTLP backend, at `OTEL_EXPORTER_OTLP_ENDPOINT`. Without the profile nothing is pushed,
  so there are no connection errors when no collector runs.
- **Metrics:** `/actuator/prometheus` is always available. Every meter carries `application=partner-onboarding`.
- **Actuator:** health with Kubernetes liveness and readiness groups, and info with build, Java and OS
  data. The build time is left out, so the jar stays reproducible.

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
  ./gradlew :onboarding-server:bootRun --args='--spring.profiles.active=dev,otel'
```

See [ADR-0011](../docs/adr/0011-observability-opentelemetry-actuator.md). In production, put actuator on a
separate management port or protect it.

## Dependencies

`onboarding-application` (and through it `partner-quality`), plus the Spring Boot web and actuator
starters, springdoc-openapi, Log4j2, and Micrometer with the OpenTelemetry bridge and OTLP and Prometheus
registries. Lombok (`@RequiredArgsConstructor`, `@Slf4j`) is used at compile
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
| `ObservabilityTest` | full context with observability | probes, info, Prometheus metrics with the application tag, OpenTelemetry tracer, no export by default, an incoming `traceparent` continued and its trace id in the log context |
| `OtlpExportTest` | full context, `otel` profile | spans really reach an OTLP endpoint (a minimal receiver on the JDK's HTTP server stands in for the collector) |
| `DemoProfileTest` | full context, `demo` profile | the seed data the README examples rely on |
| `HexagonalArchitectureTest` | ArchUnit | onion layering, adapters use ports only, no use of the library's `internal` packages |

The `test` task removes `SPRING_PROFILES_ACTIVE`, so tests never inherit the shell's `dev` profile.
Reports are in `build/reports/tests/test/index.html`.
