# partner-quality

Business-partner **data quality rules** and **duplicate detection** as a plain Java 21 library. It has no
framework dependencies and no I/O; its only runtime dependency is JSpecify (null-safety annotations).

```groovy
dependencies {
    implementation 'com.example:partner-quality:0.1.0-SNAPSHOT'
    testImplementation testFixtures('com.example:partner-quality:0.1.0-SNAPSHOT')   // optional: valid sample data
}
```

Build and install locally with `./gradlew :partner-quality:publishToMavenLocal`.

## Validate

```java
DataQualityEngine engine = DataQualityEngine.withBuiltInRules();   // immutable, thread-safe: create once

PartnerData data = new PartnerData("Acme Chemicals GmbH",
        new Address("Industriestraße 1", "10115", "Berlin", "DE"),
        "DE 136 695 976",                 // normalized to DE136695976
        "DE89 3704 0044 0532 0130 00");   // normalized to DE89370400440532013000

ValidationReport report = engine.validate(data);
report.hasErrors();    // false
report.warnings();     // []
```

Every rule runs and every finding is reported. Each `RuleViolation` has a stable `ruleId` (see `RuleIds`), a
`field` path, a `Severity` (`ERROR` blocks, `WARNING` informs) and a human-readable `message`.

| Built-in rule | `ruleId` | Checks |
|---|---|---|
| `BuiltInRules.mandatoryFields()` | `MANDATORY_FIELDS` | name, country, city required; street and postal code recommended |
| `BuiltInRules.countryCode()` | `COUNTRY_CODE` | ISO 3166-1 alpha-2 |
| `BuiltInRules.postalCodeFormat()` | `POSTAL_CODE_FORMAT` | format for AT BE CH DE ES FR GB IT NL PL US |
| `BuiltInRules.vatId()` | `VAT_ID` | prefix vs country, format, check digit (CH, DE, PL) |
| `BuiltInRules.iban()` | `IBAN` | structure, length, mod-97; warns on a foreign bank account |

### Add your own rule

```java
DataQualityRule noShouting = new DataQualityRule() {
    public String id() { return "NO_SHOUTING"; }
    public List<RuleViolation> check(PartnerData d) {
        return d.name() != null && d.name().equals(d.name().toUpperCase())
                ? List.of(RuleViolation.warning(id(), "name", "Name is all upper case"))
                : List.of();
    }
};
DataQualityEngine engine = DataQualityEngine.withBuiltInRules().with(noShouting);
```

Rules must be thread-safe and free of side effects. A rule that throws is reported as a `RULE_FAILURE`
error instead of breaking validation (fail closed).

## Find duplicates

The detector works with **your** entity type, so you keep your own model:

```java
DuplicateDetector detector = new DuplicateDetector(new MatchingPolicy(0.80, 0.95)); // or withDefaultPolicy()

List<Match<Customer>> matches = detector.findMatches(probe, candidates, Customer::partnerData);
for (Match<Customer> m : matches) {
    m.candidate();   // your Customer object, unchanged
    m.score();       // 0..1, best first
    m.reason();      // SAME_VAT_ID, SAME_IBAN or SIMILAR_NAME_AND_LOCATION
    detector.isBlocking(m);   // score >= blocking threshold
}
```

The detector compares against exactly the candidates you pass. Pre-select plausible ones ("blocking":
same country, same IBAN, a search-index query) so it scales to large datasets.

## API guarantees

- Exported packages: `com.example.partnerquality`, `.validation`, `.matching`. Anything under `.internal`
  can change in any release.
- Everything is non-null unless annotated `@Nullable` (JSpecify `@NullMarked`).
- All public types are immutable and thread-safe.
- `RuleIds` values and enum constant names are stable; violation messages may change. New `MatchReason`
  constants may be added in minor versions, so don't `switch` over it exhaustively without a default.

The principles behind these guarantees are in [docs/LIBRARY_GUIDELINES.md](../docs/LIBRARY_GUIDELINES.md).
