# Phase 8 — Timed interview walkthrough

Study this out loud. Then drill [phase-8-question-bank.md](phase-8-question-bank.md). Phase 1–7 notes still exist for depth.

If you cannot explain a line, that feature is not “yours” in an interview. You must own the why.

---

## 30 seconds

StayFinder is a hotel booking and in-room dining demo split into Spring Boot services behind one API Gateway. Auth issues RSA JWTs. Booking owns hotels and reservations in its own MySQL database. Food owns menus and orders in another database and calls Booking over Feign before accepting an order. Kafka carries email-style events to a Notification service that only logs. Eureka is the registry. Compose runs the stack.

## 60 seconds

The client never calls `:8081`–`:8084`. Gateway matches `/api/bookings/**` to `lb://booking-service` and validates JWT at the edge. Each service validates the same token again using Auth’s JWKS.

Food cannot read Booking tables. It sends the caller’s JWT on Feign. If Booking is down, Food retries, then opens a circuit and returns 503 — it does not invent a confirmed booking.

Confirming a booking commits MySQL first, then publishes `stayfinder.booking-confirmed`. HTTP 200 does not wait for email. That is the difference between synchronous validation and asynchronous side effects.

## 2 minutes

Security: Auth signs with an in-memory RSA key (restart kills tokens — I would persist keys or use an IdP in production). Roles travel in the `roles` claim. Register cannot self-assign ADMIN. 401 vs 403 vs 409 are distinct.

Booking overlap: `checkIn < existingCheckOut AND checkOut > existingCheckIn`, ignoring CANCELLED, with a pessimistic lock on the room row. Mock payment is a status change, not a payment service.

Resilience4j sits on `BookingLookupService`, not on the Feign interface, and we did not enable OpenFeign’s circuit-breaker starter because TimeLimiter would hop threads and drop `RequestContextHolder` / JWT. Retry is inner (3× GET). Circuit is outer. Fallback is fail-closed.

Kafka: JSON strings, duplicated event records, after-commit publish, at-least-once, no outbox. Notification has no Gateway route and no database.

Docker: `localhost` inside a container is that container. Compose overrides Eureka, JWKS, Kafka (`kafka:29092` vs host `localhost:9092`), and MySQL. One MySQL process, three logical databases. No Config Server.

## 5 minutes

Walk the happy path, then one failure.

1. `POST /api/auth/login` → Gateway → Auth → BCrypt → JWT.
2. `GET /api/bookings/hotels` public.
3. `POST /api/bookings` CUSTOMER → PENDING, `user_id` copied from `sub`.
4. `POST .../confirm` → CONFIRMED, afterCommit → Kafka → Notification log.
5. `POST /api/food/orders` → Feign GET booking → must be CONFIRMED and matching room → insert PLACED → Kafka food event.

Then: stop Booking, place food orders, show retries and OPEN circuit, 503. Explain 4xx must not open the circuit. Restart Booking, HALF_OPEN, CLOSED.

Then: “what I did not build” — React, Config Server, outbox, real PSP, persistent keys, three MySQL servers, mTLS.

---

## Honesty lines (use these)

| They ask | You say |
| --- | --- |
| Did you write every line? | I can explain every service, the JWT flow, Feign, Kafka after-commit, and the fail-closed circuit. I will not claim production traffic. |
| Why no React? | It is in `frontend/`. It only uses `VITE_API_BASE_URL=http://localhost:8080`. |
| Why no Config Server? | Host vs Docker is entirely hostname/env. Compose injects those. Config Server is another process I did not need on one machine. |
| Would you use Eureka at work? | On Kubernetes I would use platform DNS and a gateway. Eureka is here so I can demonstrate discovery on a laptop. |
| Is one MySQL three databases “database-per-service”? | Logically yes — Food cannot query Booking tables. Physically it is a demo. Production would isolate servers and credentials. |
| Is mock payment OK? | For a portfolio status machine, yes. I would not say I integrated Stripe if I did not. |

---

## Reverse-engineering (whole system)

1. From Gateway YAML only: which paths are public, and why JWKS is not a Gateway route that services use.
2. Why `FeignConfig` exists. What Booking returns if it is missing.
3. Why event classes are copied. What a shared JAR would couple.
4. Why `@CircuitBreaker` is not on `BookingClient`.
5. Why Kafka advertises two listeners in Compose.
6. Why Food health in Compose disables the circuit-breaker contributor.
7. Trace one CONFIRMED booking: which DBs are written, which HTTP calls run, which topic is used, what happens if Kafka send fails.
