# Phase 1 Sequence — Client to Gateway to Eureka

## Happy path after later services exist

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant E as Eureka
    participant B as Booking Service

    C->>G: GET /api/bookings/hotels
    G->>E: resolve booking-service
    E-->>G: instance host:port
    G->>B: GET /api/bookings/hotels
    B-->>G: 200 JSON
    G-->>C: 200 JSON
```

## Phase 1 actual path (no booking service yet)

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant E as Eureka

    C->>G: GET /api/bookings/hotels
    G->>E: resolve booking-service
    E-->>G: no instances
    G-->>C: 503 SERVICE_UNAVAILABLE
```

## Startup order

```mermaid
sequenceDiagram
    participant E as Eureka Server
    participant G as API Gateway

    Note over E: start first on :8761
    G->>E: register api-gateway
    loop heartbeat
        G->>E: renew lease
    end
```

If Gateway starts before Eureka, it retries registration. That usually recovers, but the reliable demo order is **Eureka first, then Gateway**.
