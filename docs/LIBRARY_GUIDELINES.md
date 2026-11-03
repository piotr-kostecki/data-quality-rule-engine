# Building a scalable, durable Java library

How to build a library that other teams can depend on for years: one that grows without breaking its
consumers and still performs at large data volumes. Each section has the principle, how
[`partner-quality`](../partner-quality) applies it, and what to add next.

> **Durable** means a consumer can upgrade a minor version without changing code.
> **Scalable** means it stays correct and fast as data volume, rule count and the number of consuming
> teams grow.

---

## Checklist

| # | Guideline | In this repo |
|---|---|---|
| 1 | One responsibility, no framework dependencies | ✅ pure Java; only runtime dep is JSpecify (annotations) |
| 2 | Small, explicit public API; everything else hidden | ✅ JPMS `module-info` exports 3 packages; `internal.*` is hidden |
| 3 | Factories and interfaces at the boundary, final classes inside | ✅ `BuiltInRules`, `DataQualityEngine.of(...)`, final engine/detector |
| 4 | Null-safety stated in the type system | ✅ `@NullMarked` packages, `@Nullable` where allowed |
| 5 | Immutable, thread-safe, no global state | ✅ records, `List.copyOf`, stateless rules |
| 6 | Expected outcomes are values, failures are isolated | ✅ `ValidationReport`; crashing rule → `RULE_FAILURE` |
| 7 | Stable identifiers are contract, prose is not | ✅ `RuleIds` constants, messages free to change |
| 8 | Generic over the consumer's types | ✅ `DuplicateDetector.findMatches(probe, pool, dataOf)` returns `Match<T>` |
| 9 | Semantic versioning, checked by a tool | ⏳ add japicmp against the last release (snippet below) |
| 10 | Strict, reproducible build | ✅ `-Xlint:all -Werror`, doclint, reproducible jars, version catalog |
| 11 | Ship sources, javadoc, test fixtures, metadata | ✅ all four published (`publishToMavenLocal`) |
| 12 | Test through the public API, with reference vectors | ✅ rule tests use `BuiltInRules.*()` |
| 13 | Property-based, mutation and performance tests | ⏳ jqwik, PIT, JMH (below) |
| 14 | Extension without forking: SPI | ⏳ `ServiceLoader` discovery of `DataQualityRule` |
| 15 | Bulk and streaming APIs for large volumes | ⏳ `Stream`-based validation and pluggable blocking |
| 16 | Documented: README, javadoc, changelog, migration notes | 🟡 README + javadoc; changelog when 1.0 ships |

---

## 1. Scope and dependencies

**Principle.** A library does one job and imposes nothing on its consumers. Every dependency you add
becomes a dependency of every consumer, and so does every version conflict.

- No frameworks in the core. Spring, Micrometer and Jackson belong in optional **adapter modules**, e.g.
  `partner-quality-spring-boot-starter` for auto-configuration and `partner-quality-micrometer` for metrics.
- Prefer the JDK: `java.util.regex`, `java.text.Normalizer`, and `System.Logger` instead of SLF4J or Log4j.
- Use `api` vs `implementation` correctly. A dependency is `api` only when its types appear in your public
  signatures (JSpecify annotations here); everything else is `implementation`, so it doesn't leak onto
  consumers' compile classpath.
- Don't shade (repackage) dependencies unless there's no other way; it hides CVEs from scanners.

**Here:** the only runtime dependency is `org.jspecify:jspecify` (a few KB of annotations).

## 2. Public API: small, explicit, hard to misuse

**Principle.** Everything public is a promise. Removing it later is a breaking change, so publish as
little as possible. It is easy to add API and very hard to take it away.

- **Separate API from implementation by package**, and make the boundary real with JPMS:
  ```java
  module com.example.partnerquality {
      requires transitive org.jspecify;
      exports com.example.partnerquality;             // PartnerData, Address
      exports com.example.partnerquality.validation;  // engine, rule SPI, results
      exports com.example.partnerquality.matching;    // detector, Match, policy
      // internal.rules and internal.text are NOT exported
  }
  ```
  Modular consumers can't compile against `internal.*`. Classpath consumers (most Spring Boot apps)
  can, so add an ArchUnit rule on the consumer side: `HexagonalArchitectureTest.nobodyUsesLibraryInternals`.
- **Factories instead of public constructors** for implementations: `BuiltInRules.vatId()` returns a
  `DataQualityRule`, so `VatIdRule` can be split, renamed or rewritten freely.
- **`final` classes by default.** A non-final public class invites subclassing, and then every protected
  or overridable method becomes API.
- **Interfaces only where consumers are meant to implement them** (`DataQualityRule`). Once published, a
  new abstract method breaks every implementor, so evolve these interfaces with `default` methods only.
- **Know which Java features change compatibility:**

  | Feature in public API | Safe change | Breaking change |
  |---|---|---|
  | `record` | add a static factory or secondary constructor | adding/removing a component breaks the canonical constructor and record patterns |
  | `enum` | — | adding a constant breaks consumers' exhaustive `switch` (document "may grow", as `MatchReason` does) |
  | `sealed` interface | — | adding a permitted subtype breaks exhaustive `switch` |
  | `interface` (implemented by users) | add a `default` method | add an abstract method |
  | `final class` | add methods | remove or change signatures |

  `PartnerData` is a record, a deliberate trade-off: concise, immutable and pattern-matchable. When it
  needs a new field, keep the old constructor as a secondary constructor that delegates with
  `null`, or switch to a builder before 1.0.
- **Generic over the consumer's types.** `DuplicateDetector.findMatches(probe, pool, dataOf)` returns the
  caller's own objects inside `Match<T>`. The application keeps its `BusinessPartner` entity, and the
  library never needs to know about it.
- **Stable identifiers are contract; prose is not.** `RuleIds.IBAN` never changes, while violation
  messages may be reworded or translated. Say so in the javadoc (see `RuleViolation`).

## 3. Null-safety, immutability, thread-safety

- **JSpecify** (`@NullMarked` per package, `@Nullable` on exceptions) makes the null contract visible to
  IDEs, NullAway, the Kotlin compiler and readers. Fail fast at the boundary with
  `Objects.requireNonNull(x, "helpful message")`.
- **Immutable values** (records with `List.copyOf` in compact constructors) can be shared freely across
  threads and cached safely.
- **Document thread-safety on every public type** ("Immutable and thread-safe; create it once and share
  it"). Consumers will put `DataQualityEngine` in a Spring singleton and call it from 200 request
  threads; it has to work.
- **No global mutable state**: no static setters, no singletons with configuration, no reading environment
  variables or system properties. Configuration enters through constructors and factories
  (`MatchingPolicy`).

## 4. Errors and resilience

- **Expected outcomes are values** (`ValidationReport`, `List<Match<T>>`). Exceptions are for programmer
  errors: a null argument, or thresholds out of range in `MatchingPolicy`.
- **Isolate faults in extension points.** One buggy custom rule must not take down validation:
  `DataQualityEngine` turns a throwing rule into a `RULE_FAILURE` error. It **fails closed** (the data is
  rejected, not silently accepted) and keeps the results of every other rule.
- **No hidden I/O.** A library that silently calls a VAT registry over HTTP hurts latency, testability and
  security reviews. External lookups belong behind an interface the consumer provides.
- **Don't log by default.** If you must, use `System.Logger`, which consumers route to their own backend.

## 5. Versioning and compatibility

- **Semantic versioning**: MAJOR.MINOR.PATCH. A minor or patch release must be **binary-compatible**
  (old consumer jars run against the new library) and should be **source-compatible**.
- **Automate the check.** Humans miss breaking changes; tools don't. With
  [japicmp](https://github.com/melix/japicmp-gradle-plugin), compare every build against the last release:
  ```groovy
  // build-logic/src/main/groovy/onboarding.library-conventions.gradle (once 0.1.0 is released)
  plugins { id 'me.champeau.gradle.japicmp' }   // version 0.4.6, declared in build-logic/build.gradle

  tasks.register('apiCompatibility', me.champeau.gradle.japicmp.JapicmpTask) {
      oldClasspath.from(configurations.detachedConfiguration(
              dependencies.create("${project.group}:${project.name}:0.1.0") { transitive = false }))
      newClasspath.from(tasks.named('jar'))
      onlyBinaryIncompatibleModified = true
      failOnModification = true
      packageIncludes = ['com.example.partnerquality', 'com.example.partnerquality.validation', 'com.example.partnerquality.matching']
  }
  tasks.named('check') { dependsOn 'apiCompatibility' }
  ```
- **Deprecate before removing**: `@Deprecated(since = "1.4", forRemoval = true)` plus javadoc pointing at
  the replacement. Keep it for at least one minor release, and remove it only in the next major.
- **Mark maturity explicitly** if parts of the API are still settling, e.g. with `@API(status = EXPERIMENTAL)`
  from apiguardian, or with a `.experimental` package excluded from the japicmp check.
- **Java baseline policy**: target an LTS release (21) and raise it only in a major version. Multi-release
  JARs can add newer-JDK optimisations without raising the baseline.
- **0.x is a licence to break, 1.0 is a promise.** Ship 1.0 once two real consumers have used the API.

## 6. Build and packaging

What this repo already does (see `build-logic/` and `gradle/libs.versions.toml`):

- **Version catalog** (`gradle/libs.versions.toml`): one place for every version, typed accessors
  (`libs.jspecify`, `libs.bundles.spring.web.server`), and Renovate/Dependabot understand it.
- **Convention plugins in Groovy** (`build-logic/src/main/groovy/*.gradle`): toolchain, compiler flags,
  test setup and publishing are written once. A new library module is a 5-line `build.gradle`.
- **Strict compilation** for published code: `-Xlint:all -Werror` and `-Xdoclint:all,-missing`.
- **Artifacts consumers expect**: main jar, `-sources`, `-javadoc`, `-test-fixtures`, POM, and Gradle
  Module Metadata (`.module`, which carries variants such as test fixtures).
- **Reproducible archives** (`preserveFileTimestamps = false`, `reproducibleFileOrder = true`): the same
  commit produces byte-identical jars, which matters for build caches and supply-chain verification.

Next steps:

- **A BOM** (`java-platform` plugin) as soon as there are two published modules, so consumers align
  versions with one line: `implementation platform('com.example:partner-quality-bom:1.2.0')`.
- **Signing and Maven Central requirements** (GPG signatures, POM `scm`/`developers`); internally,
  publish to a company repository (Nexus, Artifactory, GitHub Packages).
- **SBOM** with the CycloneDX plugin (`org.cyclonedx.bom` 3.5.1) for security scanning.
- **Dependency verification** (`./gradlew --write-verification-metadata sha256`) and locking for
  reproducible dependency resolution.

## 7. Testing a library

- **Test through the public API.** Rule tests call `BuiltInRules.vatId()`, not `new VatIdRule()`, so an
  internal refactoring never breaks tests. Whitebox tests are fine for tricky internals (`JaroWinkler`,
  `NameNormalizer`).
- **Reference vectors from the specification**: published IBAN examples, Winkler's paper values. They
  catch subtle algorithm errors that hand-made examples miss.
- **Property-based tests** (jqwik) for invariants: "changing any single digit of a valid IBAN makes it
  invalid", "similarity is symmetric and in [0, 1]", "normalizing twice changes nothing".
- **Mutation testing** (PIT, `info.solidsoft.pitest` 1.19.0) shows whether tests actually detect bugs in
  the checksum code; line coverage cannot.
- **Benchmarks** (JMH) for hot paths such as `DuplicateDetector.findMatches` over 10k candidates. Run them
  on demand, not on every build, and compare between versions.
- **A consumer smoke test**: a tiny module that depends on the *published* artifact, on the module path,
  and uses only exported packages. It catches a forgotten `exports` before your users do.
- **Ship test fixtures** (`java-test-fixtures`): `PartnerFixtures` gives consumers valid VAT IDs and IBANs,
  so they don't copy magic strings around. The server and application modules already use them.

## 8. Performance and scalability

- **Precompute what you can**: regexes are compiled once into `static final Pattern`s; lookup tables are
  immutable `Map`s.
- **Avoid needless allocation in hot paths**: the IBAN mod-97 check works digit by digit on an `int`
  rather than with `BigInteger`.
- **Stateless and shareable**: one engine instance serves every thread with no locking.
- **Keep the algorithm separate from the data access**: the detector scores whatever pool it receives.
  Narrowing the pool (blocking by country, postal code or phonetic key, or an Elasticsearch query) is the
  caller's job, so the full data set never passes through the library.
- **Bulk APIs for batch jobs**: e.g. `Stream<ValidationReport> validateAll(Stream<PartnerData>)`, so a
  cleansing job can process a file lazily without loading it into a `List`.
- **Make the expensive part pluggable**: a `SimilarityMeasure` interface (Jaro-Winkler today, token or
  phonetic measures tomorrow) and a `BlockingStrategy` let consumers tune quality and cost without forking.

## 9. Extensibility

- **SPI discovery with `ServiceLoader`**: let other jars contribute rules without changing configuration:
  ```java
  // module-info.java of the library
  uses com.example.partnerquality.validation.DataQualityRule;
  // module-info.java of a rule pack
  provides com.example.partnerquality.validation.DataQualityRule with com.acme.rules.FrenchSirenRule;
  // API
  public static DataQualityEngine withDiscoveredRules() { ... ServiceLoader.load(DataQualityRule.class) ... }
  ```
- **Rules as data** for the long tail (regex per country, value lists, cross-field conditions): a
  `ConfigurableRule` interprets versioned definitions; Java stays for algorithms like checksums.
- **Observability hooks without dependencies**: a `ValidationListener` interface (`onViolation`,
  `onRuleFailure`) that a separate `partner-quality-micrometer` module implements.

## 10. Documentation

- **README per published module** with a 10-line quick start ([partner-quality/README.md](../partner-quality/README.md)).
- **Javadoc on every exported type**, stating null policy, thread-safety and what is contract (ids) and
  what isn't (messages). `package-info.java` explains each package.
- **CHANGELOG.md** in [Keep a Changelog](https://keepachangelog.com) format, and a migration note for every
  major version.
- **ADRs** for decisions that consumers might question ([docs/adr](adr)).

## 11. Governance (when several teams depend on it)

- **CODEOWNERS** on the exported packages: every API change gets a review from the library owners.
- **API review checklist** in the PR template, based on section 2.
- **Renovate or Dependabot** on the version catalog; **OWASP/OSV scanning** in CI.
- **Support policy**: which versions get security fixes (e.g. latest minor of the last two majors).
