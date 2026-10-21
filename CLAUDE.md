# Movie Ticket Booking Platform

Learning-by-building, production-grade microservices project. Owner: Rakesh Naini (senior Java engineer, building this to learn and for interview prep).
Repo: github.com/rakeshNaini1552/movie-booking

## How to work with me
- I am learning. Before writing code for a new concept (annotation, class, pattern), explain it in 3 to 5 short sentences: what it is, why we need it here, what happens without it. No analogies. Use concrete examples from this repo.
- Work one step at a time. Finish a step, run its tests, then stop and summarise what was built and why. Wait for my go-ahead before starting the next step.
- Before changing anything, read the existing code in the service and match its patterns. Do not invent a new style.
- If a requirement is ambiguous or a design choice is not covered here, ask me one short question instead of guessing.
- Never print or commit secrets. `deploy/.env` stays out of git. Never paste env values in output.
- Do not push, merge, force-push, delete branches or run destructive commands (docker volume removal, DROP SCHEMA, topic deletion) without asking first.

## Services (all under `services/`)
| Service | Port | Role |
|---|---|---|
| seat-service | 8081 | Seat holds (Redis Lua, atomic) and DB seat state (`@Version` on ShowSeat) |
| booking-service | 8082 | Bookings, payment orchestration, transactional outbox publisher, expiry job |
| payment-service | 8083 | Mock payments |
| ticket-service | 8084 | Consumes BookingConfirmed, issues HMAC-signed QR tickets, own outbox |

Infra is in `deploy/docker-compose` (run `docker compose` from the `deploy/` folder): Kafka 4.x KRaft single broker (`kafka`, localhost:9092), one Postgres per service (ticket-postgres is on host port 5435), Redis.
Java 21, Spring Boot 4.1.x (Jackson 3: `tools.jackson.databind.json.JsonMapper`, unchecked exceptions), Spring Kafka 4.1.x, Postgres + Flyway, Testcontainers, Lombok, Maven. Base package pattern: `com.movie.booking.<service_name>`.

## Conventions
- Constructor injection. `@RequiredArgsConstructor` when no `@Value` is needed. When a constructor needs `@Value`, write the constructor by hand with `@Value` on the parameter.
- Schema changes only through Flyway migrations (`V<n>__description.sql`). Never edit an applied migration; add a new one. `spring.jpa.hibernate.ddl-auto: validate`, `open-in-view: false`.
- Inject `Clock` (bean `Clock.systemUTC()`); never call `Instant.now()` in services.
- Config values live in `application.yml` under the service's own prefix (`ticket.consumer.*`, `ticket.outbox.*`, `booking.outbox.*`) with env-var placeholders for secrets and hosts. Annotations say where behaviour applies, yml says how much and where.
- Entities: no setters. Create through a constructor or a builder annotated on the constructor (never class-level `@Builder`). Protected no-args constructor for JPA. App-assigned UUID ids.
- Events: each service has its own event record. Never share event classes between services. The JSON field names are the contract. Standard envelope fields: `eventId` (UUID), `occurredAt` (Instant), then business ids. Always spell it `occurredAt`.
- Transactional outbox in every service that publishes events: write the outbox row in the same DB transaction as the state change (`OutboxService.enqueue` has no `@Transactional` of its own and joins the caller's). A `@Scheduled` `OutboxPublisher` (conditional on `<svc>.outbox.publisher.enabled`) sends rows oldest first with key = aggregate id, header `eventType`, `acks=all`, waits for the ack, then `markSent` (`@Transactional`, relies on dirty checking). Delivery is at-least-once, so consumers must be idempotent. The outbox code is intentionally duplicated per service (no shared library).
- Idempotent consumers: check for an existing result first (`findByBookingId`), create inside one transaction with `saveAndFlush`, rely on a UNIQUE constraint as the race guard, and in the `@KafkaListener` catch `DataIntegrityViolationException` and treat it as already processed. Use conditional `UPDATE ... WHERE status = <expected>` for state transitions instead of read-modify-write.
- Consumer error handling (ticket-service `KafkaConsumerConfig`): `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries` and `DeadLetterPublishingRecoverer` writing to `ticket.consumer.dlt-topic` (`booking-events-dlt`, set explicitly because the Spring Kafka 4.x default suffix is `-dlt`). `JacksonException` is non-retryable.
- HTTP error mapping: 404 not found, 409 conflict / invalid state, 400 bad input, 503 downstream unavailable. Use a `@RestControllerAdvice` like the existing services.

## Tests
- Testcontainers with `@ServiceConnection` (`PostgreSQLContainer`, `KafkaContainer`), images pinned to the versions used in compose. Shared `TestcontainersConfiguration` (public) imported with `@Import`.
- `@SpringBootTest` for integration tests. Awaitility for anything asynchronous (never `Thread.sleep`). Unique random UUIDs per test; clean tables in `@BeforeEach`.
- Test `application.properties` in `src/test/resources` sets `<svc>.outbox.publisher.enabled=false` and any required secrets (for example `ticket.qr.secret`).
- Test effects, not implementation: rows written, events on the topic, status codes, and rollback behaviour (use `@MockitoSpyBean` with `doThrow().when()` to prove atomicity).
- Every step ends with all tests in all touched services green.

## Git workflow
- One branch per step (`git switch -c <name>` from an up-to-date `main`), one commit per branch, then a PR. Never commit to `main`.
- Commit messages: short imperative subject. Do not push; I will push and open the PR.

## Current state (Phase 5: Events and saga)
Done: Kafka in compose; BookingConfirmed contract; booking-service outbox + publisher (verified end to end); ticket-service skeleton, `ticket` table (unique `booking_id`), signed QR (`QrSigner`, HMAC-SHA256, payload `<ticketId>.<signature>`), idempotent `TicketService.issueTicket`, `@KafkaListener` consumer on `booking-events`, delayed retries + dead-letter topic, ticket-service outbox + publisher emitting `TicketIssued` to `ticket-events` (verified end to end).

## Remaining work (do in this order, one step at a time)

### Step 6b: TicketIssueFailed (branch `ticket-issue-failed`)
Goal: when ticket-service gives up on a `BookingConfirmed` message, tell the rest of the system so a later step can refund.
Design to implement (confirm with me before starting if anything conflicts with the code):
- New record `TicketIssueFailedEvent(eventId, occurredAt, bookingId, userId, reason)` in ticket-service `event` package.
- Wrap the existing `DeadLetterPublishingRecoverer` in a custom `ConsumerRecordRecoverer`. When retries are exhausted or an exception is non-retryable: try to parse `bookingId` and `userId` from the payload with `JsonMapper`. If parsing works, call a `@Transactional` service method that enqueues `TicketIssueFailed` through `OutboxService` (event type `"TicketIssueFailed"`, aggregate id = bookingId, `reason` = exception class and a truncated message). Then delegate to the dead-letter recoverer. If the payload cannot be parsed there is no booking to report: only dead-letter it and log a warning.
- Do not emit it for duplicate deliveries (those are handled as success).
- Tests: valid event whose processing keeps failing (use a `@MockitoSpyBean` on `TicketService` that throws) produces one `TicketIssueFailed` outbox row and one dead-letter record; malformed JSON produces a dead-letter record and no outbox row; a good message after a failed one still issues a ticket.

### Step 7: Ticket read and scan endpoints (branch `ticket-endpoints`)
- `GET /tickets/by-booking/{bookingId}`: returns the ticket (ticketId, bookingId, status, qrPayload, issuedAt, usedAt) or 404 until the consumer has issued it. Also `GET /tickets/{ticketId}`.
- `POST /tickets/{ticketId}/scan` with body `{ "payload": "<qr payload>" }`:
    - Verify with `QrSigner.verify`; invalid or malformed signature, or payload id different from the path id: 400.
    - Unknown ticket: 404.
    - First valid scan: atomic conditional update `status = 'USED', used_at = now WHERE ticket_id = ? AND status = 'ISSUED'`, returns 200 with status USED and `usedAt`.
    - Ticket already USED or VOID: 409 with a message that distinguishes the two and includes `usedAt` when used. (Repeated scans of the same ticket must never admit twice.)
    - Add a repository method for the conditional update returning rows affected; map 0 rows to the 409 path after re-reading the ticket.
- DTOs for responses (never return the entity), `@RestControllerAdvice`, tests with `MockMvc` or `TestRestTemplate` against Testcontainers, including a concurrency test: two simultaneous scans, exactly one succeeds.

### Step 8: Cancellation (branches per service, one PR each, order: booking, seat, ticket)
Before coding, read the existing booking, seat and payment code and show me a short plan for the flow, including which service owns which state change. Intended flow:
- booking-service: cancel endpoint for a booking. PENDING booking becomes CANCELLED (conditional update, idempotent), and the seats are released. CONFIRMED booking becomes CANCELLED, and writes `BookingCancelled` and, because it was paid, `RefundRequested` to the outbox in the same transaction. Already cancelled: return the current state (idempotent). Expired or failed bookings: 409.
- seat-service: consume `BookingCancelled` and free the seats (idempotent; a second delivery changes nothing; respect the `@Version` and conditional update rules already used there).
- ticket-service: consume `BookingCancelled` and mark the ticket VOID (conditional update from ISSUED only; a USED ticket is not voided: log and ignore). Idempotent, same retry and dead-letter handling as the existing consumer.
- `RefundRequested`: booking-service only emits it for now (payment-service consuming it is out of scope unless I ask).
- Each consumer gets its own group id and Testcontainers tests (happy path, duplicate delivery, out-of-order cases such as BookingCancelled arriving before the ticket exists: decide and document how it is handled, and ask me if unsure).

## Out of scope for now
Step 9 (saga money gaps), step 10 (notification service), outbox cleanup and multi-instance locking, security (Phase 6), observability (Phase 7).