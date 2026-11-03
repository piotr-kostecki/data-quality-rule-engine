# ADR-0005: Sealed result type for outcomes, RFC 9457 for errors

- **Status:** accepted
- **Date:** 2026-10-25

## Context

Onboarding has three expected outcomes: created, rejected by rules, duplicate. Clients (UIs, ERP
integrations) need machine-readable errors in one consistent format, including for framework errors such
as malformed JSON.

## Decision

- `OnboardingService.onboard` returns a **sealed** `OnboardingResult` (`Onboarded | Rejected |
  DuplicateSuspected`). Expected business outcomes are values, not exceptions. The controller maps them with
  an exhaustive `switch`.
- HTTP mapping:

  | Outcome | Status | Why |
  |---|---|---|
  | created | `201` + `Location` | standard for resource creation |
  | rule errors | `422 Unprocessable Content` | the syntax was fine, the content is semantically invalid |
  | duplicate | `409 Conflict` | conflicts with the current state of the resource collection |
  | malformed JSON / UUID | `400 Bad Request` | the request cannot be parsed |
  | unknown id | `404 Not Found` | |

- All errors use **RFC 9457 Problem Details** (`application/problem+json`) with a stable `type` URN plus
  extension members (`violations`, `duplicates`). `spring.mvc.problemdetails.enabled=true` makes Spring's
  own errors use the same format.

## Consequences

- ➕ The compiler forces every caller to handle every outcome.
- ➕ One error format for clients to parse.
- ➖ `ResponseEntity<?>` in the controller is less self-documenting in OpenAPI. Explicit `@ApiResponse`
  annotations would fix that if API consumers need it.

## Alternatives considered

- **Exceptions + `@ControllerAdvice`.** Common, but hides control flow and makes expected outcomes look like
  failures.
- **`200` with an `errors` array.** Breaks HTTP semantics, caches and monitoring.
