# Phase 5 Sequence — Kafka notifications

Confirm booking (happy path):

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant B as Booking Service
    participant DB as stayfinder_booking
    participant K as Kafka
    participant N as Notification Service

    C->>G: POST /api/bookings/{id}/confirm Bearer JWT
    G->>B: POST /api/bookings/{id}/confirm Bearer JWT
    B->>B: verify JWT + owner + PENDING
    B->>DB: status = CONFIRMED
    DB-->>B: commit
    B->>K: produce BookingConfirmed JSON<br/>topic stayfinder.booking-confirmed
    B-->>C: 200 CONFIRMED
    K->>N: consume (group notification-service)
    N-->>N: log Email notification sent to customer...
```

Place food order (event after Feign validation):

```mermaid
sequenceDiagram
    participant C as Client
    participant F as Food Service
    participant B as Booking Service
    participant K as Kafka
    participant N as Notification Service

    C->>F: POST /api/food/orders Bearer JWT
    F->>B: Feign GET /api/bookings/{id} (sync)
    B-->>F: 200 CONFIRMED
    F->>F: insert PLACED order
    F->>K: produce FoodOrderPlaced JSON
    F-->>C: 201 PLACED
    K->>N: consume stayfinder.food-order-placed
    N-->>N: log Email notification sent to customer...
```

Notification down (messages wait):

```mermaid
sequenceDiagram
    participant B as Booking Service
    participant K as Kafka
    participant N as Notification Service

    B->>K: BookingConfirmed
    Note over N: process stopped
    Note over K: message retained on topic
    N->>K: start + join group notification-service
    K->>N: deliver from committed offset
    N-->>N: log Email notification sent to customer...
```

Kafka down after DB commit (dual-write gap):

```mermaid
sequenceDiagram
    participant B as Booking Service
    participant DB as stayfinder_booking
    participant K as Kafka

    B->>DB: commit CONFIRMED
    B-xK: produce fails
    B-->>B: log Failed to publish...
    B-->>B: HTTP 200 still (email lost)
```
