# Movie ticket booking: handoff notes

Design doc: https://claude.ai/code/artifact/5b439eec-f506-4788-8d0f-5f11b3f7bdbf
Service: services/seat-service (package com.movie.booking.seat_service). Phase 3 is complete.
Stack: Java 21, Spring Boot, Postgres (Flyway), Redis (Lua scripts), Testcontainers.

## Conventions
- No hardcoded config. Values come from application.yml with ${ENV:default} placeholders.
- Business code depends on interfaces (HoldStore, SeatEventPublisher), not Redis. SOLID.
- Every change has a test. Tests use Testcontainers (shared TestContainersConfig).
- Small steps; the build must compile and tests must pass before the next step.
- Work on a new branch per change. Do not commit directly to main.

## Done (seat-service)
- Seat map: HELD is a response-only status for Redis-held seats (never stored in Postgres).
- Hold: all-or-none Lua script, idempotent for the same bookingId (a retry succeeds, a different
  bookingId gets 409). The hold id is the bookingId, sent by the caller.
- Extend: POST /api/v1/shows/{showId}/holds/{bookingId}/extend.
- Confirm: isHeldBy -> bookSeats -> release, no @Transactional on purpose.
- Release: SeatHoldService.releaseHold(showId, seatIds, bookingId), service method only, no endpoint.
- Expiry: every hold is also recorded in the Redis sorted set "hold:expiry". A ShedLock-guarded
  @Scheduled job claims due holds (claim_expired.lua), releases them, and reports through
  SeatEventPublisher (logs only for now). Config: seat.hold.expiry.* in application.yml.
- Tests for all of the above.

## Known limitations
- Events are at-most-once: a hold is claimed before the event is published, so a crash in between
  loses the event (the seat keys still expire by TTL). Fixed by the outbox in Phase 5.
- Releasing only some seats of a hold leaves its expiry entry behind, so a later false
  SeatsReleased(EXPIRED) event can be published. Release the full seat set of a hold.

## Next
- Phase 4: booking-service calls seat-service to hold seats, using the bookingId it creates.
- Phase 5: Kafka. Real SeatEventPublisher, outbox, ShowCreated / BookingConfirmed consumers,
  then remove the REST confirm endpoint.

## Out of scope for now
Keycloak, metrics, Docker/K8s, SSE stream, partitioned key.