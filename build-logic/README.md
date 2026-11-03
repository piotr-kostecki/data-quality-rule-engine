# build-logic

The project's own Gradle **convention plugins**. They keep the build settings that every module shares in
one place, so each module's `build.gradle` only states what kind of module it is. This is an included
build (`pluginManagement { includeBuild('build-logic') }` in the root `settings.gradle`), not a module
of the product.

[← Back to the main README](../README.md)

## Plugins

| Plugin id | Applied by | What it configures |
|---|---|---|
| `onboarding.java-conventions` | every module | Java toolchain (version from the catalog), UTF-8, `-parameters`, `-Xlint:all`; JUnit 5 + AssertJ; test logging with full stack traces and a one-line summary per module |
| `onboarding.library-conventions` | published libraries (`partner-quality`) | everything above, plus `java-library`, `-Werror`, javadoc lint, sources and javadoc jars, reproducible jars, a Maven publication with POM and licence |

Usage in a module:

```groovy
plugins {
    id 'onboarding.library-conventions'   // or onboarding.java-conventions
}
```

## How it works

- `build.gradle` applies `groovy-gradle-plugin`. Every `src/main/groovy/<id>.gradle` file becomes a
  plugin with that id.
- `settings.gradle` loads the main build's version catalog (`../gradle/libs.versions.toml`), so the
  plugins and the modules use the same versions.
- Inside the plugins, the type-safe `libs.*` accessors are not available. They use the lookup API
  instead: `versionCatalogs.named('libs').findLibrary('junit-bom')`, `findVersion('java')`.

## Changing it

- Edit the `.gradle` file and run `./gradlew build`. Gradle rebuilds the plugins automatically.
- To add a convention, create a new `src/main/groovy/<id>.gradle` and apply it by id in the modules that
  need it. Prefer this to `subprojects { }` blocks in the root build: those couple the modules and work
  against the configuration cache.

See [ADR-0008](../docs/adr/0008-gradle-groovy-version-catalog.md).
