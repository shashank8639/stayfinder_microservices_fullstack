# Phase 6 Architecture — Resilience4j on Food → Booking

## What this phase proves

Food still **must** ask Booking before accepting an order. Timeouts (Phase 4) stop a hung call. They do not stop Food from hammering a dying Booking, and they do not fail fast when Booking is clearly down.

Resilience4j sits on `BookingLookupService.getBooking`:

```text
POST /api/food/orders
        → FoodOrderService
        → BookingLookupService   (@CircuitBreaker outer, @Retry inner)
        → Feign BookingClient
        → Booking Service
```

If Booking is flaky: **retry up to 3 attempts**. If it keeps failing: the **circuit opens** and later calls skip Feign. The **fallback always rejects** the order with **503**. It never invents a CONFIRMED booking.

## Why annotations on a wrapper, not on the Feign interface

`BookingClient` is already a Feign proxy. Putting `@CircuitBreaker` on that interface is easy to get wrong.

We also **did not** set `spring.cloud.openfeign.circuitbreaker.enabled=true`. That integration wraps Feign in Spring Cloud CircuitBreaker, which uses a **TimeLimiter** on another thread by default. `FeignConfig` reads `Authorization` from `RequestContextHolder` (thread-local). A thread hop would drop the JWT and Booking would return 401. Timeouts stay on the Feign client: 2s connect, 3s read.

## Retry vs timeout

| | Timeout | Retry |
| --- | --- | --- |
| Question | How long may **one** HTTP attempt run? | How many times do we try the whole call? |
| StayFinder | Feign 2s / 3s | `max-attempts: 3`, 200ms apart |

Retries exist because a single timeout or 503 is often a blip. They are dangerous on **non-idempotent writes** (double charge). `GET /api/bookings/{id}` is safe to retry. We **do not** retry 400/401/403/404 — those will not improve.

## Circuit breaker states

```text
CLOSED  → calls go to Booking. Failures are counted in a sliding window of 8.
          After 4 calls, if ≥50% failed → OPEN

OPEN    → Feign is not called. Fallback runs immediately (fail fast).
          After 10s → HALF_OPEN

HALF_OPEN → a few probe calls (2) are allowed.
            Success → CLOSED. Failure → OPEN again.
```

4xx types are ignored in `BookingResilienceConfig` (YAML cannot reliably name `FeignException$NotFound`). A missing booking must not open the circuit.

## Fallback (fail closed)

`bookingUnavailable` rethrows 4xx (a missing booking is not an outage) and otherwise throws `BookingDependencyException`. The API returns 503 `"Booking Service is unavailable"`.

The `@CircuitBreaker` fallback method runs for **any** exception from the decorated call, including ignored 4xx. Rethrowing those is required so Food can still map 404/403 correctly. `ignoreExceptions` on the circuit still matters so 404s do not open it.

A fallback that returned a fake `BookingView(CONFIRMED)` would place room service for a guest who may not have a room. That hides a serious failure. Interview answer: **fail closed**.

## Aspect order

CircuitBreaker aspect order **1** (outer), Retry order **2** (inner):

```text
request → CircuitBreaker → Retry (up to 3) → Feign
```

One customer request that exhausts retries counts as **one** circuit failure, not three. When OPEN, Retry never runs.

## Simplifications

- No Bulkhead (we are not limiting concurrent Feign calls)
- No TimeLimiter (would hop threads and break JWT relay)
- Circuit state is in-memory per Food instance (no Redis)
- `/actuator/circuitbreakers` is permitAll locally
