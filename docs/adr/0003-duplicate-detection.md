# ADR-0003: Duplicate detection with normalization, Jaro-Winkler and identifier evidence

- **Status:** accepted
- **Date:** 2026-10-25

## Context

The same company appears under many spellings ("ACME Chemicals G.m.b.H.", "Acme Chemicals GmbH"). Exact
matching misses these duplicates. Refusing everything that looks a bit similar blocks legitimate partners,
for example two subsidiaries in different cities.

## Decision

1. **Normalize** names: Unicode NFC, lower case, transliterate umlauts and `ł`, strip diacritics and
   punctuation, remove legal forms (`NameNormalizer`).
2. **Compare** normalized names with **Jaro-Winkler**, which is designed for short strings and tolerates
   typos and transpositions.
3. **Weight** with location: `0.85 × name + 0.15 × location`, where location is 1.0 (same postal code and
   city), 0.7 (same city), 0.5 (unknown) or 0.0 (different city).
4. **Identifiers win**: same VAT ID scores 1.0; same IBAN scores 0.90 (strong, but group companies share
   accounts).
5. **Two thresholds**, both configurable in `application.yml`: `candidate-threshold` (0.80) reports a
   possible duplicate; `blocking-threshold` (0.95) refuses onboarding.
6. **Blocking**: only partners in the same country, or with the same IBAN, are compared.

## Consequences

- ➕ Explainable: every candidate has a score and a `reason`, which data stewards need.
- ➕ Tunable without a release.
- ➖ The weights are hand-picked. They should be validated against labelled data, and eventually learned.
- ➖ Word order matters ("Logistik Müller" vs "Müller Logistik"). A token-based measure would fix that.
- ➖ Country-based blocking misses cross-border duplicates without a shared IBAN.

## Alternatives considered

- **Exact match on normalized name.** Simple and fast, but misses typos.
- **Levenshtein distance.** Works, but penalises differences at the start and end equally; less suited to
  names.
- **ML / embeddings.** Best quality at scale, but needs training data and infrastructure. A natural
  evolution once steward decisions are being collected.
