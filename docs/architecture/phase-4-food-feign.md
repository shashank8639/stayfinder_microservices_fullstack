# Phase 4 Architecture — Food Service + OpenFeign

## What this phase proves

Food Service is MS-2. It owns menu and food orders in `stayfinder_food`. It does **not** query Booking's database. Before placing an order it calls Booking Service over HTTP using OpenFeign, forwarding the customer's JWT.

```text
Customer JWT
    → Gateway
    → Food Service
    → Feign GET /api/bookings/{id}  (same Authorization header)
    → Booking Service validates JWT again and ownership
    → Food Service checks CONFIRMED + hotel + room
    → insert order in stayfinder_food
```

## Why Feign, not JDBC into booking_db

That would break database-per-service. Food would couple to Booking's schema. Feign keeps a DTO boundary: `BookingView` is Food's copy of the fields it needs, not Booking's JPA entity.

## Why this Booking endpoint is not "a secret Feign API"

`GET /api/bookings/{bookingId}` is a normal authenticated API: the owner (or ADMIN) can read their booking. Food reuses it. The JWT is the trust: Booking will 403 if the token is not the owner.

## What Feign actually does

`@EnableFeignClients` scans `@FeignClient` interfaces. Spring Cloud OpenFeign creates a JDK proxy. On `bookingClient.getBooking(10)` the proxy builds:

```text
GET http://<booking-service-instance>/api/bookings/10
Authorization: Bearer <relayed JWT>
```

The host comes from Eureka (`lb://booking-service`). `FeignConfig` copies the incoming Authorization header because Feign does **not** forward headers by default.

Timeouts are set (2s connect, 3s read). Retry and Circuit Breaker are **Phase 6**. If Booking is down now, Food returns **503**.

loggerLevel BASIC logs method and URL, not the JWT.

## Validation rules

| Check | Result if failed |
| --- | --- |
| Booking 404 | 404 |
| Booking 403 / other user | 403 |
| Status not CONFIRMED | 400 |
| hotelId mismatch | 400 |
| roomNumber provided and mismatch | 400 |
| Booking Service down | 503 |

`bookingId` is required on a food order so Feign is always exercised. Walk-in dining without a booking is out of scope.

## Simplifications

- Resilience4j Retry + CircuitBreaker + fail-closed 503 fallback (Phase 6)
- HOTEL_OWNER can see all hotel food orders
- Menu `hotelId` is a Long, not an FK to Booking's hotels table
- `FoodOrderPlaced` Kafka event is Phase 5; this phase only does the Feign write path
