# ADR-0010: Lombok in application and server code, not in published libraries

- **Status:** accepted
- **Date:** 2026-11-03

## Context

Spring components and use-case services repeat the same boilerplate: constructors that only assign
`final` fields, and logger declarations. Value types are already Java `record`s. Lombok removes this
boilerplate at compile time. It changes what the source shows, though, and generated members carry no
javadoc.

## Decision

- Lombok is applied by the convention plugin `onboarding.lombok-conventions` (`compileOnly` +
  `annotationProcessor`, for main and test code). Its version comes from the version catalog and is
  kept equal to the Spring Boot BOM's.
- Modules that use it: `onboarding-application` and `onboarding-server`.
- **Not** in `partner-quality` or any other published library. Their public API is records and
  hand-written, documented code, and consumers read the sources jar.
- Allowed annotations: `@RequiredArgsConstructor` for constructor injection, and `@Slf4j` for loggers
  (SLF4J API, see ADR-0009). Prefer `record` over `@Value` or `@Data`.
- `lombok.config` at the root marks generated code with `@lombok.Generated`, so coverage tools skip it.
  It makes `val`, `var`, `@SneakyThrows` and experimental features a compile error.

## Consequences

- ➕ Less boilerplate in components; a new dependency is just a new `final` field.
- ➕ Nothing on the runtime classpath, and no Lombok types in the bytecode of the hexagon's public types.
- ➖ IDEs need Lombok support. IntelliJ bundles it (enable annotation processing). VS Code and Neovim
  run jdtls with the Lombok agent (LazyVim's Java extra does this automatically).
- ➖ The source no longer shows every member. Keep usage to the allowed annotations above.
