# Phase 5 Architecture — Kafka + Notification Service

## What this phase proves

Booking and Food already completed their HTTP work when a booking is confirmed or a food order is placed. Telling the customer about that is **not** part of the same request. Notification Service listens on Kafka and logs `"Email notification sent to customer..."`. There is no real email provider.

```text
Customer confirms booking
    → Booking writes CONFIRMED in stayfinder_booking
    → transaction commits
    → Booking publishes JSON BookingConfirmed to stayfinder.booking-confirmed
    → HTTP 200 returns to the customer
    → Notification Service consumes the message (later, independently)
    → log: Email notification sent to customer...
```

Food does the same on `stayfinder.food-order-placed`.

## Why Kafka, not Feign

Feign is **synchronous**: Food must know the booking is valid *before* it accepts the order. Kafka is **asynchronous**: the guest already has a confirmed booking; email can lag by a second without blocking checkout.

If Notification is down, Booking still confirms. Messages wait on the topic. That is the point of a broker.

Calling Notification with Feign would couple checkout latency and availability to the mailer. That is the wrong communication style for this use case.

## What we publish

Event **records**, not JPA entities. A `Booking` entity has lazy `Room`/`Hotel` and Hibernate proxies. JSON of that would leak persistence details and break when the schema changes.

| Topic | Producer | When |
| --- | --- | --- |
| `stayfinder.booking-confirmed` | Booking Service | PENDING → CONFIRMED (`/confirm` or mock `/payment/confirm`) |
| `stayfinder.food-order-placed` | Food Service | order saved as PLACED |

PENDING create does **not** publish. Status updates on food (PREPARING / DELIVERED) do **not** publish.

The event class is **copied** in producer and consumer. A shared `stayfinder-events` JAR would couple deployability. Schema Registry is a production improvement, out of scope.

## After commit, not in the same DB transaction

`BookingEventPublisher` registers `TransactionSynchronization.afterCommit`. The DTO is built while the booking (with JOIN FETCH hotel/room) is still loaded. The Kafka send happens **after** MySQL commits.

That avoids: notify, then rollback, then the customer got an email for a booking that does not exist.

It does **not** solve the dual-write problem: DB commits, then Kafka is down, then the email is lost. Production uses an **outbox** table in the same transaction, then a relay publishes to Kafka. StayFinder logs the send failure and continues. Booking confirm still returns 200.

## Delivery

Spring Kafka `ack-mode: record` + `enable-auto-commit: false`: the offset is committed after the listener method returns. If the process crashes after logging but before the commit, Kafka redelivers. That is **at-least-once**. Logging twice is acceptable here. Exactly-once needs idempotency keys or Kafka transactions — out of scope.

Poison JSON is caught and logged. The offset still advances so one bad message does not stall the partition. A dead-letter topic is the production alternative.

## Notification Service

- Port **8084**, registers with Eureka
- **No** public business API and **no** Gateway route
- **No** database
- `/actuator/health` is public; anything else is 403
- Consumer group `notification-service`

Two Notification instances with the same group and **one** partition: only one instance receives each message. Partitions are how Kafka scales consumers in a group.

## Local Kafka

Phase 7 is the full Docker Compose stack. Phase 5 only needs a broker (host JVM):

```bash
docker compose -f docker-compose.kafka.yml up -d
```

Do not run that file together with `docker-compose.yml` — both publish `9092`. KRaft mode — no ZooKeeper. Host apps: `localhost:9092`. Compose apps: `kafka:29092` (dual listeners in the full stack file).

Tests use `@EmbeddedKafka`. They do not need Docker.

## Simplifications

- Log instead of SMTP / SES
- No outbox, no Schema Registry, no Kafka transactions
- One partition, one replica
- Kafka health is disabled on Booking/Food so a down broker does not mark those services DOWN
