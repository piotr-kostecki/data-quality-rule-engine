# ADR-0008: Gradle Groovy DSL, version catalog and convention plugins

- **Status:** accepted
- **Date:** 2026-10-26

## Context

With three modules, copying versions and build configuration into each `build.gradle` drifts quickly: one
module upgrades JUnit, another forgets. The library also needs stricter settings and publishing, which the
server must not get.

## Decision

- **Groovy DSL** (`settings.gradle`, `build.gradle`) for all build scripts.
- **Version catalog** `gradle/libs.versions.toml` holds every external version, library coordinate, bundle
  and plugin id. Build files reference `libs.jspecify`, `libs.bundles.spring.web.server` and
  `alias(libs.plugins.spring.boot)`. Spring-managed libraries carry no version; the Spring Boot BOM
  (`platform(libs.spring.boot.bom)`) provides it.
- **Convention plugins** as Groovy precompiled script plugins in the included build `build-logic/`:
  - `onboarding.java-conventions`: Java 21 toolchain, `-parameters -Xlint:all`, JUnit 5 + AssertJ.
  - `onboarding.library-conventions`: on top of that, `java-library`, `-Werror`, doclint, sources and
    javadoc jars, reproducible archives, `maven-publish`.
  They read versions through `versionCatalogs.named('libs')`; `build-logic/settings.gradle` imports the
  same TOML file.
- Repositories are declared once in `settings.gradle` with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- `gradle.properties` enables the build cache, parallel execution and the configuration cache.

## Consequences

- ➕ One-line version upgrades; Renovate/Dependabot understand the TOML catalog.
- ➕ A new module is a short build file that applies one convention plugin.
- ➕ BOM import via `platform()` replaces the `io.spring.dependency-management` plugin: plain Gradle
  semantics and one plugin fewer.
- ➖ Precompiled Groovy script plugins can't use the type-safe `libs` accessor, so they look entries up by
  name (`libs.findLibrary('junit-jupiter').get()`). A typo fails at configuration time, not in the IDE.
- ➖ Groovy DSL has weaker IDE completion than the Kotlin DSL. Chosen for familiarity; switching later is
  mechanical.
