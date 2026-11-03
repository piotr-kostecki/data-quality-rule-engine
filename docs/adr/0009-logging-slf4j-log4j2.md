# ADR-0009: Logging with the SLF4J API and Log4j2, JSON output for Logstash

- **Status:** accepted
- **Date:** 2026-11-03

## Context

The service's logs will be collected centrally, with Logstash or an equivalent ingest pipeline. That needs
machine-readable output. Spring Boot's default backend is Logback. We want a backend that is flexible to
configure (appenders, layouts, routing, async logging) and can emit a Logstash-compatible format without
custom code. Libraries must not force a logging backend on their consumers.

## Decision

- Application code logs through the **SLF4J API** only, and never imports a backend class.
- The server uses **Log4j2** as the backend (`spring-boot-starter-log4j2`), and `spring-boot-starter-logging`
  (Logback) is excluded from every configuration.
- `onboarding-server/src/main/resources/log4j2-spring.xml` defines the output:
  - by default, human-readable lines on stdout;
  - with the `json-logs` profile, one JSON object per line on stdout. The format is
    `LogstashJsonEventLayoutV1` (Log4j2 `JsonTemplateLayout`), plus a `service` field.
- Logs always go to stdout. Shipping them (Filebeat or Fluent Bit to Logstash) is the platform's job, not
  the application's.
- Log levels stay in `application*.yml` (`logging.level.*`).
- `partner-quality` has no logging dependency at all. It reports outcomes as values.

## Consequences

- ➕ Switching to another format (ECS, GELF) is a one-line change of `eventTemplateUri`.
- ➕ Switching the backend again touches only the server's build file and configuration, never the code.
- ➕ Log4j2 offers async loggers, routing and many appenders (e.g. a socket appender sending directly to
  Logstash) when they are needed.
- ➖ Logback must stay excluded from every new starter. Without the exclusion there would be two SLF4J
  backends on the classpath.
- ➖ Trace and correlation ids are not in the logs yet. They come with OpenTelemetry (DESIGN.md §14).
