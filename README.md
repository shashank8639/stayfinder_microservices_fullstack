# StayFinder — Day 4: Kafka notifications

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL · Kafka

StayFinder is a hotel booking and in-room dining platform. **This commit series is Day 4:** after a booking is confirmed or a food order is placed, Booking/Food publish JSON events to Kafka. Notification Service consumes them and **logs** an email line. There is no SMTP provider and no Notification route on the Gateway. Docker Compose for the full stack and React are not in this tree yet.

## What Day 4 proves

Confirming a booking is an HTTP write to `stayfinder_booking`. Telling the guest is **not** a second Feign call. After the MySQL commit, Booking publishes `stayfinder.booking-confirmed`. Food publishes `stayfinder.food-order-placed`. Notification Service (port 8084) is a consumer only.

```text
POST /confirm  → DB CONFIRMED  → after commit  → Kafka  → Notification log
HTTP 200 returns without waiting for that log
```

If Notification is down, Booking still returns 200. Messages wait on the topic.

## Modules

| Application | Port | Role |
| --- | --- | --- |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Only public HTTP entry |
| `auth-service` | 8081 | Users, RSA JWT, JWKS |
| `booking-service` | 8082 | Hotels, rooms, bookings; produces booking-confirmed |
| `food-service` | 8083 | Menu, orders, Feign; produces food-order-placed |
| `notification-service` | 8084 | Kafka consumer, log-only email; **no Gateway route** |

## Run locally

Start Kafka first (KRaft, no ZooKeeper). Do not run this file together with a full-stack Compose file — both use port 9092.

```bash
docker compose -f docker-compose.kafka.yml up -d

export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Eureka, Auth, Booking, Food, Notification, then Gateway
```

Confirm a booking or place a food order through the Gateway, then watch **notification-service** logs for:

`Email notification sent to customer`

Requests: [http/phase-5.http](http/phase-5.http)

## Docs

- [Architecture](docs/architecture/phase-5-kafka.md)
- [Sequence diagrams](docs/sequence-diagrams/phase-5-kafka.md)
- [Interview notes](docs/interview-notes/phase-5-kafka.md)

## Later (not in this repo yet)

Full Docker Compose stack, React.
