# StayFinder API

All **business** HTTP goes through the Gateway: `http://localhost:8080`.

The React app (`frontend/`) uses `VITE_API_BASE_URL=http://localhost:8080`. Do not point it at `:8081`–`:8084`.

JWT: `Authorization: Bearer <accessToken>` from `POST /api/auth/login`.

Statuses: **401** missing/invalid JWT · **403** wrong role or not owner · **409** overlap / duplicate email · **400** validation or unconfirmed booking for food · **503** Booking unavailable (Food circuit/fallback).

Runnable copies: `http/phase-1.http` … `http/phase-8.http`.

## Public (no JWT)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/register` | Always CUSTOMER |
| POST | `/api/auth/login` | Returns `accessToken` |
| GET | `/api/auth/health` | |
| GET | `/api/auth/.well-known/jwks.json` | Prefer Auth `:8081` from services, not via Gateway |
| GET | `/api/bookings/hotels` | |
| GET | `/api/bookings/hotels/{hotelId}/rooms` | |
| GET | `/api/bookings/rooms/{roomId}/availability?checkIn=&checkOut=` | |
| GET | `/api/food/hotels/{hotelId}/menu` | |

## Auth

| Method | Path | Role |
| --- | --- | --- |
| GET | `/api/auth/me` | any authenticated |
| GET | `/api/auth/admin-check` | ADMIN |

Register body: `{ "email", "password" }` (password 8–72 chars).  
Login body: `{ "email", "password" }`.  
Response: `{ "accessToken", "tokenType": "Bearer", "user": { "id", "email", "roles" } }`.

## Booking — CUSTOMER

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/bookings` | `{ "roomId", "checkIn", "checkOut" }` → PENDING |
| GET | `/api/bookings/my` | |
| GET | `/api/bookings/{bookingId}` | owner or ADMIN |
| POST | `/api/bookings/{id}/confirm` | PENDING → CONFIRMED, Kafka event |
| POST | `/api/bookings/{id}/payment/confirm` | mock; same status change as confirm |
| POST | `/api/bookings/{id}/cancel` | frees dates |

Overlap of non-CANCELLED bookings on the same room → **409**.

## Booking — ADMIN / HOTEL_OWNER

| Method | Path |
| --- | --- |
| POST | `/api/bookings/hotels` `{ "name", "city", "description?" }` |
| POST | `/api/bookings/rooms` `{ "hotelId", "roomNumber", "type", "pricePerNight" }` |
| PUT | `/api/bookings/rooms/{roomId}` |
| GET | `/api/bookings/hotel/{hotelId}` |

## Food — CUSTOMER

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/food/orders` | requires CONFIRMED booking (Feign) |
| GET | `/api/food/orders/my` | |

Order body:

```json
{
  "hotelId": 1,
  "bookingId": 1,
  "roomNumber": "101",
  "items": [{ "menuItemId": 1, "quantity": 1 }]
}
```

Booking must be CONFIRMED; `hotelId` / `roomNumber` must match; caller must own the booking. Booking down → **503**.

## Food — ADMIN / HOTEL_OWNER

| Method | Path |
| --- | --- |
| POST | `/api/food/menu` `{ "hotelId", "name", "price", "available" }` |
| PUT | `/api/food/menu/{itemId}` |
| DELETE | `/api/food/menu/{itemId}` |
| GET | `/api/food/hotel/{hotelId}/orders` |
| PUT | `/api/food/orders/{orderId}/status` `{ "status": "PREPARING" \| "DELIVERED" }` |

Order statuses: `PLACED` → `PREPARING` → `DELIVERED`. Status updates do **not** publish Kafka.

## Actuator (local demo)

| URL | Why |
| --- | --- |
| `http://localhost:8080/actuator/health` | Gateway |
| `http://localhost:8761` | Eureka dashboard |
| `http://localhost:8083/actuator/circuitbreakers` | Food circuit state |
| `http://localhost:8084/actuator/health` | Notification (Kafka) |

## Kafka (not HTTP)

| Topic | JSON record (copied per service) |
| --- | --- |
| `stayfinder.booking-confirmed` | bookingId, userId, guestEmail, hotelId, hotelName, roomNumber, checkIn, checkOut, occurredAt |
| `stayfinder.food-order-placed` | orderId, userId, guestEmail, hotelId, bookingId, roomNumber, itemCount, occurredAt |
