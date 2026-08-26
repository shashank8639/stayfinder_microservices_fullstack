# Phase 5 Interview Notes — Kafka notifications

You should be able to explain producer vs consumer, topic vs partition, consumer group vs offset, at-least-once, and why this is not a Feign call.

---

## 30-second explanation

When a booking is confirmed, Booking Service writes CONFIRMED to its own database, then publishes a JSON `BookingConfirmed` event to Kafka. Food does the same for `FoodOrderPlaced`. Notification Service is a Kafka consumer. It has no public business API. It logs that an email was sent. The customer does not wait for that log.

## 60-second explanation

A **topic** is a named stream (`stayfinder.booking-confirmed`). A **partition** is an ordered log inside that topic. We use one partition locally. The **producer** (Booking) appends a JSON record with the booking id as the key. The **consumer** (Notification) is in group `notification-service`. Kafka stores an **offset** per group per partition: “how far has this group read?”.

Spring Kafka commits the offset after the listener method returns (`ack-mode: record`). If Notification crashes after logging and before that commit, Kafka redelivers. That is **at-least-once**. Duplicate logs are acceptable. Exactly-once needs extra design.

We publish **after the DB commit** so we never email for a rolled-back booking. If Kafka is down after commit, the email is lost. That dual-write gap is why production uses an outbox.

## 2-minute explanation

StayFinder uses two communication styles on purpose:

| Need | Style | Example |
| --- | --- | --- |
| Must know the answer before continuing | Synchronous Feign | Food asks Booking: is this booking CONFIRMED and mine? |
| Side effect that can happen later | Asynchronous Kafka | Email that the booking was confirmed |

If email used Feign, checkout would fail whenever Notification was down. With Kafka, Booking confirms anyway. Notification catches up when it starts.

We serialize **event DTOs**, not JPA entities. Hibernate proxies and lazy collections do not belong on a topic. Producer and consumer each have their own copy of the record. Jackson ignores unknown fields on the consumer, so Booking can add a field without immediately breaking Notification.

There is no SMTP. The log line is the stand-in for “we would send email here.”

---

## Short interview answers

### What is a Kafka topic?

**Short interview answer**

A named, append-only stream of records. Producers write; consumers read.

**Deeper explanation**

A topic is split into partitions. Each partition is an ordered log. Ordering is guaranteed **per partition**, not across the whole topic. The record key chooses the partition (hash). StayFinder keys by booking id / order id so one entity stays on one partition.

**Example from StayFinder**

`stayfinder.booking-confirmed` and `stayfinder.food-order-placed`.

**Likely follow-up**

- Why two topics instead of one `stayfinder.events` topic?
- What happens with two partitions and two consumers in the same group?

**Strong answer**

Separate topics keep schemas and retention independent. Two partitions with group `notification-service` split the work: each partition is owned by one consumer in the group. Two instances and one partition: one instance is idle.

### What is a consumer group?

**Short interview answer**

A named team of consumers that share work on a topic. Each partition is assigned to at most one member of the group. Kafka tracks one offset per group per partition.

**Deeper explanation**

A second group on the same topic gets its own offsets — both groups see all messages. That is how you fan out: email in `notification-service`, analytics in another group later.

**Example from StayFinder**

`group-id: notification-service`.

### What is at-least-once delivery?

**Short interview answer**

The consumer may see the same message more than once. It will not silently skip a message that was processed but not committed.

**Deeper explanation**

After the listener returns, Spring commits the offset. Crash in between → redelivery. At-most-once commits first (can lose messages). Exactly-once needs idempotent consumers and/or Kafka transactions.

**Example from StayFinder**

A duplicate “Email notification sent…” log is fine. Charging a card twice would not be.

### Why not Feign to Notification Service?

**Short interview answer**

The customer already succeeded. Email is a side effect. Coupling it to HTTP would make Booking as available as the mailer.

**Deeper explanation**

Synchronous calls are for questions that gate the response. Async events are for “something happened; whoever cares can react.” Notification has no Gateway route because React should never call it.

### Why publish after commit, not in the same DB transaction?

**Short interview answer**

So a rolled-back booking never produces an email. Kafka is not in the MySQL transaction.

**Deeper explanation**

`TransactionSynchronization.afterCommit` runs after MySQL commit, before the HTTP response is fully done. The event DTO is built inside the transaction while JOIN FETCH associations are loaded. This still loses messages if Kafka is down after commit. An **outbox** table written in the same SQL transaction is the production fix.

### Why duplicate the event record in three modules?

**Short interview answer**

Services should not share a domain JAR. The JSON field names are the contract.

**Deeper explanation**

A shared module looks tidy until Booking cannot deploy without rebuilding Notification. Schema Registry + versioned Avro/JSON Schema is the grown-up version of the same idea.

---

## Reverse-engineering drills

1. Confirm a booking with Notification stopped. Start Notification. Do you still see the email log? Why?
2. Confirm a booking with Kafka stopped. What does the customer get? What is in the database? What is in the logs?
3. Publish `Booking` (the JPA entity) instead of `BookingConfirmedEvent`. What breaks?
4. Give Notification `group-id: something-else` and run two instances. Who gets the message?
5. Throw from the listener instead of catching JSON errors. What happens to the next message on that partition?
6. Move `kafkaTemplate.send` to **before** `setStatus(CONFIRMED)` and then throw. Did the customer get an email for a PENDING booking?
7. Call Notification with Feign from Booking. List two production problems that Kafka avoided.
8. Why is there no `/api/notifications` route on the Gateway?

## Common failures

| Symptom | Cause |
| --- | --- |
| Confirm 200, no email log | Notification down, wrong topic, wrong consumer group, Kafka down after commit |
| Booking tests hang / fail connecting to 9092 | Kafka auto-config not excluded; publisher not mocked |
| Email for a booking that rolled back | Published inside the transaction instead of afterCommit |
| Listener never fires in tests | `auto-offset-reset` not `earliest`, or message sent before listener joined |
| Hibernate LazyInitializationException in JSON | Published the entity after the session closed |
