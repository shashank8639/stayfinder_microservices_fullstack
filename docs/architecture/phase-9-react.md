# Phase 9 Architecture — React on the Gateway only

## What this phase proves

A browser app can exercise StayFinder without knowing any service port. The only HTTP base URL is the API Gateway.

```text
React :5173
  → VITE_API_BASE_URL=http://localhost:8080
  → Gateway
  → Auth / Booking / Food
```

There is still **no** call to `:8081`–`:8084`, Eureka, Kafka, or Notification. Notification remains log-only.

## Why one base URL

If React had `VITE_BOOKING_URL=http://localhost:8082`, the Gateway would be optional and CORS/JWT would multiply. The interview line is: **the frontend is a Gateway client**.

Gateway already allows CORS `http://localhost:*` (Vite’s 5173). We did **not** add a Vite proxy — that would hide the Gateway URL.

## JWT in the browser (demo)

| Step | What happens |
| --- | --- |
| Login | `POST /api/auth/login` → `{ accessToken, user }` |
| Store | `localStorage` keys `stayfinder.token` and `stayfinder.user` |
| Axios | request interceptor sets `Authorization: Bearer` |
| 401 | interceptor clears storage (not on login/register itself) |
| Logout | same clear |
| ProtectedRoute | UX only — hides `/bookings`, `/orders`, `/admin` |

**This is not security.** A crafted request to `:8080` still needs a valid JWT; each service checks again. `localStorage` is XSS-vulnerable. Production: httpOnly cookie or a BFF. Auth restart still kills tokens (in-memory RSA).

## Pages

| Page | Auth | APIs |
| --- | --- | --- |
| Hotels / Rooms / Menu | public GETs | catalog + menu |
| Login / Register | public POSTs | `/api/auth/*` |
| My bookings / orders | JWT | create/confirm/cancel, food orders |
| Staff dashboard | ADMIN or HOTEL_OWNER | rooms, menu, hotel bookings, order status |

Register still always creates CUSTOMER.

## Simplifications

- No design system, no Redux, no React Query
- Token in localStorage
- React is **not** in Docker Compose (`npm run dev` on the host)
- Staff UI is one dashboard, not a full PMS
