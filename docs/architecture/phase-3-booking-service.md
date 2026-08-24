# Phase 3 Architecture — Booking Service

## What this phase proves

Booking Service is MS-1. It owns hotels, rooms and bookings in **its own MySQL database** `stayfinder_booking`. Auth's database is never queried. The user id in a booking is just a Long copied from the JWT `sub` claim — not a foreign key to `stayfinder_auth`. That is database-per-service.

Public catalog reads do not need a JWT. Creating a booking does. Booking Service validates JWT itself via JWKS even though Gateway already did.

## Why this service exists

Hotels and availability are a different business capability from login. They will later be called by Food Service through Feign. If Booking is down, Auth and later Food's catalog-unrelated work can still run. Isolation is the point of the portfolio.

## Overlap rule

A room is unavailable when an existing **non-cancelled** booking overlaps:

```text
newCheckIn < existingCheckOut
AND newCheckOut > existingCheckIn
```

Cancelled bookings are ignored. There is no temporary hold. Concurrent creates lock the room row (`PESSIMISTIC_WRITE`) so two customers cannot double-book in the same transaction window. That is a small production-style detail, not a distributed lock service.

## Status flow

```text
POST /api/bookings                → PENDING
POST /.../payment/confirm (mock)  → CONFIRMED
POST /.../confirm                 → CONFIRMED
POST /.../cancel                  → CANCELLED
```

Payment is mock: no Payment entity, no Razorpay. Confirming payment just marks the booking CONFIRMED. Kafka `BookingConfirmed` is published after that commit — see Phase 5.

## Layers

| Layer | Why |
| --- | --- |
| controller | HTTP mapping, JWT principal, `@PreAuthorize` |
| service | overlap, ownership, status rules |
| repository | SQL/JPQL, room lock |
| entity | JPA tables |
| dto | API contract — entities never leave the service |
| mapper | entity → dto |
| security | resource server + role converter |

## Dual JWT validation

```text
GET /api/bookings/hotels          public at Gateway and Booking
POST /api/bookings                Gateway 401 if no JWT; Booking checks CUSTOMER role
POST /api/bookings/hotels         CUSTOMER → 403 at Booking (@PreAuthorize)
```

HOTEL_OWNER is not scoped to “their” hotels yet. Any HOTEL_OWNER can manage catalog. Production would filter by owner id. Simplification.

## What we did not add

- Payment table / gateway
- Complex inventory holds
- Searching every city with Elasticsearch
- Calling Auth Service to verify the user still exists (JWT signature is enough for this phase)
