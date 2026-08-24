# StayFinder — Day 3: Booking Service

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL

StayFinder is a hotel booking and in-room dining platform. **This commit series is Day 3:** hotels, rooms, bookings, overlap detection, and JWT checks inside Booking. Food, Kafka consumer, Docker Compose, and React are not in this tree yet.

## What Day 3 proves

A client still talks to **one URL**: `http://localhost:8080`. Public hotel catalog needs no JWT. Creating a booking needs a CUSTOMER token. Booking Service owns MySQL database `stayfinder_booking` and does **not** query Auth's tables. The booking row stores `userId` copied from the JWT `sub` claim.

Overlapping **non-cancelled** bookings return **409**. The room row is locked (`PESSIMISTIC_WRITE`) while inserting so two requests cannot double-book in the same window.

```text
GET /api/bookings/hotels          → 200, no JWT
POST /api/bookings                → 401 without JWT, 201 PENDING with CUSTOMER JWT
POST /api/bookings (overlap)      → 409
POST /api/bookings/hotels         → 403 with CUSTOMER JWT
```

Payment confirm is **mock**: it only sets status `CONFIRMED`. There is no Stripe or Razorpay.

## Modules

| Application | Port | Role |
| --- | --- | --- |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Only public HTTP entry |
| `auth-service` | 8081 | Users, RSA JWT, JWKS |
| `booking-service` | 8082 | Hotels, rooms, bookings |

## Demo catalog (seeded on Booking startup)

| Hotel | City |
| --- | --- |
| Grand Horizon | Hyderabad |
| Creek Palace | Dubai |
| Fort View | Jaipur |

## Demo users (Auth)

| Email | Password | Role |
| --- | --- | --- |
| `customer@stayfinder.local` | `Customer@123` | CUSTOMER |
| `admin@stayfinder.local` | `Admin@123` | ADMIN |
| `owner@stayfinder.local` | `Owner@123` | HOTEL_OWNER |

## Run locally

MySQL must be running on `127.0.0.1:3306`. Set `MYSQL_PASSWORD` in the shell if needed. Do not commit `.env`.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Terminal 1
cd eureka-server && mvn spring-boot:run

# Terminal 2
cd auth-service && mvn spring-boot:run

# Terminal 3
cd booking-service && mvn spring-boot:run

# Terminal 4
cd api-gateway && mvn spring-boot:run
```

- Eureka dashboard: http://localhost:8761 — you should see `API-GATEWAY`, `AUTH-SERVICE`, `BOOKING-SERVICE`
- Public catalog: `curl -s http://localhost:8080/api/bookings/hotels`
- Expected 401: `curl -i -X POST http://localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"roomId":1,"checkIn":"2026-09-10","checkOut":"2026-09-12"}'`

More requests: [http/phase-3.http](http/phase-3.http)

## Docs

- [Architecture](docs/architecture/phase-3-booking-service.md)
- [Sequence diagrams](docs/sequence-diagrams/phase-3-booking-flow.md)
- [Interview notes](docs/interview-notes/phase-3-booking-service.md)
- Day 2: [Auth + JWT](docs/architecture/phase-2-auth-jwt.md)
- Day 1: [Eureka + Gateway](docs/architecture/phase-1-eureka-gateway.md)

## Later days (not in this repo yet)

Food + Feign, Kafka notifications, Resilience4j, Docker Compose, React.
