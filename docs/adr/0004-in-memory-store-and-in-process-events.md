# ADR-0004: In-memory store and in-process events for now

- **Status:** accepted (to be revisited before production)
- **Date:** 2026-10-25

## Context

The task requires tests that run without infrastructure, while keeping persistence replaceable. A
production system would use a database and a message broker.

## Decision

- `adapter.out.persistence.InMemoryPartnerRepository`: a `ConcurrentHashMap` implementing the
  `LoadPartnerPort` and `SavePartnerPort` outgoing ports.
- `adapter.out.event.SpringEventPublisherAdapter`: Spring's `ApplicationEventPublisher` behind the
  `PublishPartnerEventPort` outgoing port.
- `OnboardingService.onboard` is `synchronized`, so the check-for-duplicates-then-save sequence is atomic
  **within one JVM**.

## Consequences

- ➕ Zero setup; the full test suite runs in about 10 seconds.
- ➖ Data is lost on restart.
- ➖ `synchronized` serialises all onboardings and does nothing across several instances.
- ➖ In-process events are lost when the process crashes, and they are not visible to other services.

## Path to production

1. **MongoDB adapter** (`adapter.out.persistence.MongoPartnerRepository`, implementing the same two ports) with indexes on `address.country` and `iban`, and a **unique sparse index on
   `vatId`**. The database then guarantees VAT uniqueness across instances, and `synchronized` can go.
   Fuzzy duplicates created concurrently are rare; catch them with an asynchronous deduplication job
   instead of a global lock.
2. **Transactional outbox**: store the event in the same transaction as the partner, and have a relay
   (poller or Debezium) publish it to Kafka. This avoids the dual-write problem (DB committed, event lost).
3. **Idempotent consumers**, because the outbox delivers at least once.
4. Integration tests with **Testcontainers** for MongoDB and Kafka.
