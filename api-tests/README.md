# API tests

Functional tests for the running HTTP API of [`onboarding-server`](../onboarding-server), in two tools.
Both suites cover the same flow and can be run again and again against the same server: each run onboards
its own partner with a random name.

[← Back to the main README](../README.md) · Next steps for testing: [DESIGN.md §21](../DESIGN.md#21-api-and-end-to-end-testing-next-steps)

| Folder | Tool | Format |
|---|---|---|
| [`soapui/`](soapui) | [SoapUI](https://www.soapui.org) open source 5.x | one project file, `partner-onboarding-soapui-project.xml` |
| [`bruno/`](bruno) | [Bruno](https://www.usebruno.com) | a collection of plain-text `.bru` files, one per request, with environments |

Manual requests for IntelliJ or VS Code REST Client are in [`../http/requests.http`](../http/requests.http).

## What is tested

| # | Request | Expected |
|---|---|---|
| 1 | `GET /actuator/health` | `200`, `status = UP` |
| 2 | `GET /v3/api-docs` | `200`, documents `/api/v1/business-partners` (and `/checks` in Bruno) |
| 3 | `POST /api/v1/business-partners` with a new, valid partner | `201`, the partner as sent, a `Location` header pointing to it |
| 4 | `GET /api/v1/business-partners/{id}` of that partner | `200`, same id and name |
| 5 | `POST /api/v1/business-partners/checks` with the same data | `200`, `onboardable = false`, the partner as the duplicate |
| 6 | `POST /api/v1/business-partners` with a bad postal code, VAT check digit and foreign IBAN | `422` problem details listing every failed rule (Bruno also checks `application/problem+json`) |
| 7 | `GET /api/v1/business-partners/{unknown id}` | `404` problem details |
| 8 | `POST /api/v1/business-partners` with malformed JSON | `400` problem details |
| 9 | `GET /api/v1/business-partners?country=pl` | `200`, contains the new partner, only PL partners |
| 10 | *(Bruno, demo data only)* `GET /api/v1/business-partners` | the three demo partners |
| 11 | *(Bruno, demo data only)* the demo spelling-variant check | `ACME Chemicals G.m.b.H.` is a duplicate of `Acme Chemicals GmbH` |

## Start the server first

See the [quickstart](../README.md#quickstart-run-the-server):

```bash
./gradlew :onboarding-server:bootRun                       # dev profile in the Nix shell: with demo data
SPRING_PROFILES_ACTIVE= ./gradlew :onboarding-server:bootRun   # no profile: empty store
```

## Bruno

Open the `bruno/` folder as a collection in the Bruno app (**Open Collection**) and pick an environment. Or
use the Bruno CLI (`bru`, package `@usebruno/cli`) from inside `bruno/`:

```bash
cd api-tests/bruno
bru run --env local-dev                                      # all 11 requests
bru run --env local-default                                  # requests 10–11 are skipped
bru run --env local-dev --reporter-junit build/bruno-junit.xml   # JUnit XML for CI
```

Environments (`bruno/environments/`) match the server's Spring profiles:

| Environment | Server started with | `baseUrl` | Demo checks |
|---|---|---|---|
| `local-dev` | `dev` profile (Nix shell default) | `http://localhost:8080` | run |
| `local-default` | no profile | `http://localhost:8080` | skipped |
| `local-dev-8081` | `java -jar … --spring.profiles.active=dev --server.port=8081` | `http://localhost:8081` | run |

To add an environment for another server, copy one of these files and change `baseUrl` and `demoData`.
Environment files can't contain comments: the Bruno parser rejects a `#` line there. Document an
environment in the table above instead.
Never commit secrets: Bruno reads them from a local, git-ignored `.env` file via `{{process.env.NAME}}`.

## SoapUI

In the SoapUI app: **File → Import Project** → `soapui/partner-onboarding-soapui-project.xml`, then run the
test suite **Onboarding API smoke**. The target server is the project property `baseUrl` (default
`http://localhost:8080`).

Headless, e.g. in CI, with the test runner shipped with SoapUI (needs Java 17+):

```bash
<soapui-home>/bin/testrunner.sh -r -j -f build/soapui-reports \
    -PbaseUrl=http://localhost:8080 api-tests/soapui/partner-onboarding-soapui-project.xml
```

`-j` writes JUnit XML into the `-f` folder. `-PbaseUrl=…` points the suite at another server.

## Status

- **SoapUI:** run with the SoapUI 5.10.0 test runner against the server with the `dev` profile. Result:
  1 test case, 9 steps, 24 assertions, 0 failed.
- **Bruno:** run with Bruno CLI 4.3.0. Results:
  - `local-dev` against the server with the `dev` profile: 11/11 requests, 8/8 tests, 22/22 assertions,
    and the same again on a second run against the same server;
  - `local-default` against a server without a profile: 9 passed, 2 demo requests skipped, 17/17 assertions;
  - `local-dev` against that server without demo data fails as it should: the two demo checks fail.
