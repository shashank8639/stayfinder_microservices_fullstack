# Phase 6 Sequence — Retry, circuit breaker, fallback

Transient Booking failure (retry succeeds):

```mermaid
sequenceDiagram
    participant C as Client
    participant F as Food Service
    participant L as BookingLookupService
    participant B as Booking Service

    C->>F: POST /api/food/orders
    F->>L: getBooking(id)
    L->>B: Feign GET /api/bookings/id
    B--xL: timeout / 503
    Note over L: Retry attempt 2
    L->>B: Feign GET /api/bookings/id
    B-->>L: 200 CONFIRMED
    L-->>F: BookingView
    F-->>C: 201 PLACED
```

Booking down (retries exhausted, then circuit opens):

```mermaid
sequenceDiagram
    participant C as Client
    participant F as Food Service
    participant L as BookingLookupService
    participant B as Booking Service

    C->>F: POST /api/food/orders
    F->>L: getBooking(id)
    loop max 3 attempts
        L->>B: Feign GET
        B--xL: 503 / connection error
    end
    L-->>L: fallback → BookingDependencyException
    F-->>C: 503 Booking Service is unavailable
    Note over L: After 4 such requests (50% of window): CLOSED → OPEN
    C->>F: POST /api/food/orders
    F->>L: getBooking(id)
    Note over L: OPEN — Feign skipped
    L-->>F: fallback 503
    F-->>C: 503 Booking Service is unavailable
```

Wrong fallback (what we did **not** do):

```text
fallback returns fake CONFIRMED BookingView
        → Food inserts PLACED order
        → guest may not have a room
```

StayFinder fails closed instead.
