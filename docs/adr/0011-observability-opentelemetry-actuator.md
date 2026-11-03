# ADR-0011: Observability with Actuator, Micrometer and OpenTelemetry (OTLP)

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The service has to be operable in Kubernetes and visible in a central observability stack: traces across
services, metrics, and logs that can be joined with traces. The backends are not chosen yet (Grafana
stack, Elastic, a vendor), so the application must speak an open standard rather than a vendor protocol.

## Decision

- **Instrumentation:** Spring's Micrometer Observation API, which already instruments HTTP server
  requests, with the **OpenTelemetry bridge** (`micrometer-tracing-bridge-otel`). There is no Java agent
  and no OTel-specific code in the application.
- **Export:** **OTLP/HTTP**, OpenTelemetry's protocol, for traces (`opentelemetry-exporter-otlp`) and
  metrics (`micrometer-registry-otlp`). It is enabled by the `otel` profile, and the endpoint comes from
  the standard `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://localhost:4318`). Without the profile nothing
  is pushed, so local runs and tests log no connection errors.
- **Metrics pull:** `/actuator/prometheus` (`micrometer-registry-prometheus`) is always available, for
  setups that scrape.
- **Propagation:** W3C Trace Context (`traceparent`). An incoming sampled trace is always continued. New
  traces are sampled at 10% by default, and at 100% with `dev` and `otel`.
- **Log correlation:** Micrometer Tracing puts `traceId` and `spanId` into the MDC. The text log pattern
  prints `[traceId,spanId]`, and the Logstash JSON layout carries them in its `mdc` object (ADR-0009).
- **Actuator:**
  - Kubernetes probes `/actuator/health/liveness` and `/actuator/health/readiness`;
  - `/actuator/info` with build information (without the build time, so jars stay reproducible), Java and OS;
  - `/actuator/metrics`;
  - all meters tagged with `application`.
- All versions come from the Spring Boot BOM; the version catalog only names the artifacts.

## Consequences

- ➕ The backend can be swapped without code changes: point `OTEL_EXPORTER_OTLP_ENDPOINT` at a Collector
  and route from there.
- ➕ One trace id joins an HTTP request, its log lines and downstream calls.
- ➖ Logs are not exported over OTLP yet. They go to stdout (ADR-0009) and are joined with traces by id.
- ➖ The rule engine library itself is not instrumented yet. Per-rule timings come with its `RunListener`
  seam (DESIGN.md §14).
- ➖ `/actuator/prometheus` and `/actuator/info` are unauthenticated. In production, expose actuator on a
  separate management port or protect it.
