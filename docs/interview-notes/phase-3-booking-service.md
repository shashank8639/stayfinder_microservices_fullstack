# Phase 3 Interview Notes — Booking Service

You should be able to explain database-per-service, overlap, why Booking validates JWT again, and 401 vs 403 vs 409.

---

## 30-second explanation

Booking Service is a separate Spring Boot app on port 8082 with its own MySQL database `stayfinder_booking`. It stores hotels, rooms and bookings. Catalog GETs are public. Creating a booking requires a CUSTOMER JWT. Overlapping non-cancelled bookings return 409. Auth data is never queried; we only copy `sub` and `email` from the token.

## 60-second explanation

Database-per-service means Booking cannot JOIN to Auth's `users` table. The booking row has `user_id BIGINT` — a copy of the JWT subject. If Auth deletes a user, Booking still has history. Consistency is eventual and application-level, not a foreign key.

Availability uses `checkIn < existingCheckOut AND checkOut > existingCheckIn`, excluding CANCELLED. We lock the room row while inserting to reduce double-booking races. We do not use a two-phase distributed transaction.

Gateway permits public hotel GETs. Everything else still needs a JWT at the edge. Booking Service is the authorization authority for booking operations.

## 2-minute explanation

Controllers do not contain overlap logic. `BookingService.createBooking` validates dates, locks the room, counts overlaps, then inserts PENDING. Confirm and mock payment both move PENDING → CONFIRMED. Cancel makes the dates free again.

`@PreAuthorize("hasRole('CUSTOMER')")` on create booking is authorization. Missing JWT never reaches that annotation — the filter chain returns 401 first. A valid ADMIN JWT on create booking is 403 because the role is wrong. A CUSTOMER JWT confirming someone else's booking is 403 from `AccessDeniedException` in the service.

No Payment entity: mock payment is a status change. Interviewers may ask if that is production. Answer honestly: no. It demonstrates an endpoint and a status machine without integrating a PSP.

---

## Short interview answers

### Why not query Auth's database for the user?

**Short interview answer**

That would couple schemas. Booking would break if Auth renamed a column. Database-per-service keeps ownership clear.

**Example from StayFinder**

`stayfinder_auth` vs `stayfinder_booking` on the same MySQL server. Same server is a demo convenience. Isolation is still logical.

### Why 409 for overlap, not 400?

**Short interview answer**

400 means the request is malformed. Overlap is a valid request that conflicts with current state. 409 Conflict is the right code.

### Why pessimistic lock?

**Short interview answer**

Two customers can pass the overlap check at the same moment. Locking the room row serializes the inserts. It is not distributed locking across services.

---

## Reverse-engineering drills

1. Remove `AND b.status <> CANCELLED`. What false rejection appears?
2. Remove `@Lock(PESSIMISTIC_WRITE)`. What race remains?
3. Expose the `Booking` entity from the controller. What leaks?
4. Let CUSTOMER send `userId` in the body instead of JWT `sub`. What bug is that?
5. Make Gateway permitAll for all `/api/bookings/**`. What still protects writes?
6. Why does GET `/api/bookings/hotels` return 200 without a token, while GET `/api/bookings/my` returns 401?

## Common failures

| Symptom | Cause |
| --- | --- |
| 401 on hotel list | Gateway public GET matcher missing after restart |
| 503 on hotel list | Booking not registered in Eureka |
| 401 with valid token | Auth restarted, old JWT, or JWKS URI wrong |
| 409 immediately | Demo overlap or leftover PENDING booking |
| LazyInitializationException | Mapper accessed hotel outside a transaction |

## Honesty

Do not claim a real payment platform or multi-hotel owner isolation. Say: "Pending → confirmed is a mock status change. Owner-scoped hotels would be a production improvement."
