# Phase 6 Interview Notes — Resilience4j

You should be able to draw CLOSED → OPEN → HALF_OPEN, explain why we retry GET but not 404, and why fallback returns 503.

---

## 30-second explanation

Food calls Booking through Feign. `BookingLookupService` retries a failed lookup up to three times. If Booking keeps failing, the circuit opens and Feign is skipped. The fallback rejects the food order with 503. We never accept an order without a real CONFIRMED booking.

## 60-second explanation

Timeouts bound **one** HTTP attempt. Retries handle **blips**. A circuit breaker stops us from adding load to a service that is already down, and fails fast instead of waiting for three timeouts on every request.

StayFinder ignores 4xx for both Retry and the circuit (`BookingResilienceConfig`): “booking not found” is a business answer, not a Booking outage. Fallback is fail-closed: 503, not a fake booking.

## 2-minute explanation

The Feign client stays a plain HTTP proxy so `RequestContextHolder` still has the JWT. Resilience4j AOP wraps a separate Spring bean. CircuitBreaker is the outer aspect; Retry is inner. One user request that fails three times is one circuit failure.

States: CLOSED (normal), OPEN (short-circuit to fallback for 10s), HALF_OPEN (probe). Logs show `Retrying Booking lookup` and `Booking circuit CLOSED -> OPEN`. Actuator: `GET /actuator/circuitbreakers`.

Demo: stop Booking, place a few food orders, watch retries then OPEN, then 503 without Feign. Start Booking, wait for HALF_OPEN, next success closes the circuit.

---

## Short interview answers

### Why retries?

**Short interview answer**

A single timeout or 503 is often transient. Retrying an **idempotent GET** can succeed without failing the customer.

**Deeper explanation**

Retries amplify load. On a POST that charges a card they can double-bill. Food retries only the booking **read**. 404/403 are not retried.

**Likely follow-up**

Retry vs timeout? Timeout = duration of one attempt. Retry = how many attempts.

### Why can retries be dangerous?

**Short interview answer**

They multiply traffic to a sick dependency and can repeat non-idempotent side effects.

**Example from StayFinder**

Three retries × many Food instances while Booking is restarting. The circuit exists so that amplification stops once failure rate is high.

### What are the circuit breaker states?

**Short interview answer**

CLOSED: calls pass, failures counted. OPEN: calls fail immediately via fallback. HALF_OPEN: limited probes; success closes, failure opens again.

**Deeper explanation**

StayFinder: window 8, minimum 4 calls, 50% failure threshold, 10s open, 2 probes. Thresholds are demo-sized so you can trip the circuit in a few curls.

### What is fallback?

**Short interview answer**

The method that runs when the circuit is open or the decorated call still failed after retries.

**StayFinder**

Throws `BookingDependencyException` → HTTP 503. We do **not** return a stub CONFIRMED booking.

### Why not hide the failure?

**Short interview answer**

In-room dining without a confirmed booking is a business bug. 503 is honest. A cached last-known booking would be a different, documented product decision — not this demo.

### How do you demonstrate this in an interview?

Stop `booking-service`. Place food orders. Logs: retries, then `CLOSED -> OPEN`. Further orders are 503 immediately. `GET http://localhost:8083/actuator/circuitbreakers` shows OPEN. Start Booking, wait ~10s, one success, CLOSED.

---

## Reverse-engineering drills

1. Put `@CircuitBreaker` on `BookingClient` instead of `BookingLookupService`. What breaks?
2. Enable `spring.cloud.openfeign.circuitbreaker.enabled=true` without disabling TimeLimiter. Why might Booking return 401?
3. Remove 404 from ignore-exceptions **and** from fallback rethrow. Trip the circuit by requesting missing bookings. Why is that wrong?
4. Change fallback to return `new BookingView(..., "CONFIRMED")`. What orders get created?
5. Swap aspect order so Retry is outer. How many circuit failures is one 503 request?
6. Retry a non-idempotent `POST /pay`. What production incident is that?
7. Open the circuit, then call `GET /api/food/hotels/1/menu`. Does the menu still work? Why?

## Common failures

| Symptom | Cause |
| --- | --- |
| 401 from Booking after adding CB | TimeLimiter / extra thread dropped JWT |
| 503 on a missing booking | 404 counted as failure / retried into fallback |
| Circuit never opens | `minimum-number-of-calls` too high, or 4xx ignored when you expected 503 |
| Tests hang | Retry wait-duration still 200ms × many calls; test YAML should use 1ms |
| Annotations ignored | Self-invocation (`this.getBooking`) or missing `spring-boot-starter-aop` |
