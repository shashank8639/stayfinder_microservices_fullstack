# StayFinder — Day 3: Booking, then Food + OpenFeign

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL

StayFinder is a hotel booking and in-room dining platform. **This commit series is still Day 3:** first Booking (hotels, overlap, 409), then Food so in-room dining can check a **CONFIRMED** stay over HTTP. Kafka consumer, Docker Compose, and React are not in this tree yet.

## What this day proves

A client still talks to **one URL**: `http://localhost:8080`.

**Booking** owns `stayfinder_booking`. Overlapping non-cancelled bookings return **409**. Catalog GETs need no JWT.

**Food** owns `stayfinder_food`. It does **not** query Booking's database. Before placing an order it calls Booking with **OpenFeign**, forwarding the same JWT. The stay must be **CONFIRMED**. If Booking is down, Food returns **503** (fail-closed).

```text
POST /api/bookings     → PENDING → confirm → CONFIRMED
POST /api/food/orders  → Feign GET /api/bookings/{id}  → 201 if CONFIRMED
                         Booking down                 → 503
```

Payment confirm is **mock**. There is no Stripe or Razorpay.

## Modules

| Application | Port | Role |
| --- | --- | --- |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Only public HTTP entry |
| `auth-service` | 8081 | Users, RSA JWT, JWKS |
| `booking-service` | 8082 | Hotels, rooms, bookings |
| `food-service` | 8083 | Menu, orders, Feign to Booking |

## Demo users (Auth)

| Email | Password | Role |
| --- | --- | --- |
| `customer@stayfinder.local` | `Customer@123` | CUSTOMER |
| `admin@stayfinder.local` | `Admin@123` | ADMIN |
| `owner@stayfinder.local` | `Owner@123` | HOTEL_OWNER |

## Run locally

MySQL must be running on `127.0.0.1:3306`. Start Eureka, Auth, Booking, Food, then Gateway.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

cd eureka-server && mvn spring-boot:run
cd auth-service && mvn spring-boot:run
cd booking-service && mvn spring-boot:run
cd food-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

- Public catalog: `curl -s http://localhost:8080/api/bookings/hotels`
- Public menu: `curl -s http://localhost:8080/api/food/hotels/1/menu`
- Book + confirm first, then place a food order (see [http/phase-3.http](http/phase-3.http) and [http/phase-4.http](http/phase-4.http))

## Docs

- Booking: [architecture](docs/architecture/phase-3-booking-service.md) · [interview](docs/interview-notes/phase-3-booking-service.md)
- Food + Feign: [architecture](docs/architecture/phase-4-food-feign.md) · [interview](docs/interview-notes/phase-4-food-feign.md) · [sequence](docs/sequence-diagrams/phase-4-feign-flow.md)

## Later (not in this repo yet)

Kafka notifications, Docker Compose, React.
