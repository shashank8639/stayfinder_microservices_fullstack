# Phase 4 Interview Notes — Food Service + OpenFeign

You should be able to explain why Food does not touch Booking's database, what a Feign proxy does, and how the JWT is relayed.

---

## 30-second explanation

Food Service stores menus and orders in `stayfinder_food`. Placing an order calls Booking Service through OpenFeign to confirm the booking exists, belongs to the caller, is CONFIRMED, and matches hotel/room. That HTTP call uses the same customer JWT. Food never opens Booking's MySQL schema.

## 60-second explanation

OpenFeign is a declarative HTTP client. You write an interface with `@FeignClient(name = "booking-service")` and `@GetMapping`. At startup Spring creates a proxy. The name is a Eureka service id, not a hostname. `lb://` resolution picks an instance. A `RequestInterceptor` copies `Authorization` from the incoming servlet request onto the Feign request. Without that interceptor, Booking would see an anonymous call and return 401.

We did not invent a hidden internal API. `GET /api/bookings/{id}` is a real owner-read endpoint. Food is just another HTTP client.

## 2-minute explanation

Synchronous communication is used because the order must not be accepted if the booking is invalid. That is a request/response question, not an event. Kafka would be too late: the customer would already have an order.

If Booking is down, Phase 4 returns 503. That is honest. Phase 6 will add Retry, Circuit Breaker and a documented fallback. Silently accepting the order would hide a serious failure.

DTO boundary: Food deserializes `BookingView` (id, hotelId, roomNumber, userId, status). It does not import Booking's `Booking` entity. Extra JSON fields are ignored.

---

## Short interview answers

### What is OpenFeign?

**Short interview answer**

A Spring Cloud library that turns an annotated interface into an HTTP client proxy, optionally discovered via Eureka.

**Deeper explanation**

The proxy uses a contract (Spring MVC annotations), an encoder/decoder (Jackson), a client (typically JDK or Apache), and a load balancer when the name is a service id.

**Example from StayFinder**

`BookingClient.getBooking(id)` becomes `GET /api/bookings/{id}` to `booking-service`.

**Likely follow-up**

- Feign vs RestTemplate vs WebClient?
- How do you pass headers?

**Strong answer**

RestTemplate/WebClient are imperative. Feign is declarative and maps well to a small number of service-to-service APIs. Headers are not copied automatically; StayFinder uses a RequestInterceptor. WebClient would be the reactive alternative.

### Why not query booking_db?

**Short interview answer**

That couples schemas and destroys independent deployability. Food would break when Booking renames a column.

### Why forward the user JWT instead of a service account?

**Short interview answer**

Booking's authorization is "this user owns this booking". The user's token is the proof. A service-to-service token would need Booking to trust Food blindly. mTLS or a dedicated internal scope is a production improvement, currently out of scope.

---

## Reverse-engineering drills

1. Remove `FeignConfig` interceptor. What status does Booking return?
2. Change `@FeignClient(name = "booking-service")` to a hardcoded `url = "http://localhost:8082"`. What did you lose?
3. Accept PENDING bookings in Food. Why is that a business bug?
4. Let Food read `stayfinder_booking` with a second datasource. What architecture rule did you break?
5. Log Feign at FULL. Why is that dangerous?
6. If Booking returns extra JSON fields, why does Food still work?

## Common failures

| Symptom | Cause |
| --- | --- |
| 401 from Booking during Feign | Authorization not relayed |
| 503 from Food | Booking down or not in Eureka |
| 400 "requires CONFIRMED" | Forgot payment/confirm |
| UnknownHost / no instances | food-service started before booking registered |
| Feign decode error | `BookingView` field names do not match JSON |

## Honesty

Do not say you implemented a service mesh or mTLS. Say: "Synchronous Feign with JWT relay. Timeouts plus Resilience4j Retry and CircuitBreaker on Booking lookup. Fallback is 503, fail closed."
