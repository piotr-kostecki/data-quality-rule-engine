# Take-Home Task: Data-Quality Rule Engine

We do **not** expect every requirement finished — we

care far more about design, the quality of what you do complete, and how you

reason about trade-offs. Leave notes on what you'd do with more time.

---

## 1. Context

We validate large volumes of **business-partner master data** (company records:

names, addresses, tax identifiers, bank accounts, …) against a catalog of

**data-quality rules**. A record can be checked by dozens of rules.

Your task is to design and implement a **reusable Java library** that runs this

validation. The library is embedded by several hosts (an HTTP API, batch

workers, scheduled jobs).

We are giving you **requirements, not a design.** How you structure the code,

which types you introduce, and which libraries you use are yours to decide and

defend.

---

## 2. Domain model

- **Record** — one business partner, supplied as a JSON object. A batch is many

  records.

- **Rule** — a named check that computes a **decision** for a record purely from

  the record's own fields. A rule has:

  - a stable **id** and human **label**;

  - logic that computes a **value** from the record's fields;

  - a declarative **decision mapping** from that value → decision, with a

    `default` fallback;

  - metadata: **status** (`DRAFT`/`RELEASED`/`DEPRECATED`), **severity**

    (`ERROR`/`WARNING`/`INFO`), optional **country scope**, and zero or more

    **categories**.

- **Decision** — one of `VALID`, `INVALID`, `REVIEW`, `NOT_APPLICABLE`.

- **Result** — what a rule produced for a record: the decision plus enough

  **provenance** to explain it (at least: which rule, which record, and which

  field(s)/value(s) were evaluated).

---

## 3. Functional requirements

### 3.1 Core — validate a batch

- **F1 Make a Result for each rule.**

  - The engine reads a batch of records and the rule catalog.

  - The engine runs each applicable rule on each record.

  - When a rule produces an outcome, the engine makes one **Result**.

  - A **Result** carries the **decision** (`VALID`/`INVALID`/`REVIEW`/

    `NOT_APPLICABLE`), the rule's **severity** (`ERROR`/`WARNING`/`INFO`), and

    the **provenance**.

  - The provenance explains the decision: which rule, which record, and which

    field(s)/value(s) the rule read.

- **F2 Derive the decision through a declarative mapping, not if/else.**

  - A rule computes a **value** from the record's fields — for example

    `"blocked"` for a blocked country, or `"ok"`/`"bad"` for a format check.

  - A declarative **decision mapping** turns that value into a **decision**

    (e.g. `"blocked" → INVALID`), with a `default` for anything unmapped.

  - A decision is one of `VALID`, `INVALID`, `REVIEW`, `NOT_APPLICABLE`.

  - Keep the mapping as data. Do not hard-code the decision with if-then-else

    inside the rule.

- **F3 Isolate faults.**

  - A rule can fail: bad logic, a bad record, or a missing field.

  - When a rule fails, the engine records a per-rule / per-record **error** with

    enough detail to locate the cause.

  - One failure must not stop the batch or the run.

  - The caller receives both the results and the errors.

- **F4 Give a run summary.**

  - The number of results and the number of errors.

  - A breakdown of results by **decision** (how many `VALID`, `INVALID`,

    `REVIEW`, `NOT_APPLICABLE`).

  - A breakdown of results by **severity** (`ERROR`/`WARNING`/`INFO`).

### 3.2 Extended — scale and selection

- **F5 Process large volumes.**

  - The target is about 1,000,000 records per run.

  - Memory use must stay bounded regardless of total volume.

  - The engine emits each result as it is produced. It must not accumulate all

    results in one list.

  - The batch size is configurable by the caller.

- **F6 Select a subset of the rules.**

  - The caller restricts a run with composable filters that combine.

  - Filter by **status** (`DRAFT` / `RELEASED` / `DEPRECATED`).

  - Filter by **country scope**: a rule applies when its scope is `WORLD` or its

    country equals the record's country.

  - Filter by **category** (a rule has zero or more categories).

  - Whatever the filters select, the engine runs `RELEASED` rules only.

---

## 4. Non-functional requirements

- Plain Java 21 (or later).

- Build with Gradle.

- Keep the source in GitHub with a sensible commit history.

- Choose your own tools and libraries.

- *Other non-functional requirements were deliberately left out — consider what

  else a good library is expected to provide.*

---

## 5. Provided fixtures

Implement so that **this scenario passes** (adapt field/format details to your

model; keep the observable behaviour).

**Records (JSON):**

```json
[
  {
    "id": "r1",

    "vatId": "DE111111111",

    "country": "DE",

    "legalName": "ACME GmbH",

    "iban": "DE123456789"
  },

  {
    "id": "r2",

    "vatId": "FR22",

    "country": "FR",

    "legalName": "Bricolage SARL",

    "iban": "FR123456789"
  },

  {
    "id": "r3",

    "country": "ZZ",

    "legalName": "Nowhere Ltd",

    "iban": ""
  }
]
```

**Rules** (all self-contained, all `RELEASED`):

| id | logic (computed value) | decision mapping |

|------------------|------------------------------------------------------------------|---------------------------------------------|

| `countryBlocked` | `"blocked"` if `country == "ZZ"`, else `"ok"` | `blocked→INVALID`, `default→NOT_APPLICABLE` |

| `vatFormat` | `"ok"` if `vatId` present and length ≥ 9, else `"bad"` | `ok→VALID`, `bad→INVALID` |

| `ibanFormat` | `"ok"` if `iban` present and starts with `country`, else `"bad"` | `ok→VALID`, `bad→INVALID` |

**Expected results:**

| record | `countryBlocked` | `vatFormat` | `ibanFormat` |

|--------|------------------|--------------------|----------------------|

| r1 | NOT_APPLICABLE | VALID | VALID |

| r2 | NOT_APPLICABLE | INVALID (len 4) | VALID |

| r3 | INVALID | INVALID (no vatId) | INVALID (empty iban) |

Also demonstrate **fault isolation** — introduce one rule/record that fails and

show the rest of the batch still completes with the failure reported.

---

## 6. Deliverables

1. **Source** — buildable with Gradle, Java 21.

2. **A short `DESIGN.md`** (this matters as much as the code) covering:

   - your public API and why it's shaped that way;

   - how you achieved fault isolation and bounded memory;

   - the seams you introduced for host-supplied inputs;

   - trade-offs you made under the time-box and **what you'd do next**.

3. **Tests**, and instructions to run them.

4. **Example usage** — a small HTTP API that uses the library to validate a

   batch of records and return the results.

---

## 7. What we evaluate

- **Library quality** — did they anticipate what makes a library good to embed

  and depend on?

- **Correctness** of the core scenario and the declarative decision mapping.

- **Fault isolation** — results _and_ errors, nothing sinks the run.

- **Scale thinking** — streaming vs. accumulation; bounded memory.

- **Extensibility** — clean seams for catalog sources and rule selection.

- **Judgement & communication** — the `DESIGN.md`, the trade-offs, the tests.

We would rather see a **smaller slice done to a high standard** with clear

reasoning than a broad, shaky sketch. If you make an assumption, state it.
