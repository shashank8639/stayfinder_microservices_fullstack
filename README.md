# StayFinder — Day 2: Auth + RSA JWT

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL

StayFinder is a hotel booking and in-room dining platform. **This commit series is Day 2:** users, BCrypt passwords, RSA-signed JWT, and Gateway JWT validation. Booking, Food, Kafka, and React are not in this tree yet.

## What Day 2 proves

A client talks to **one URL**: `http://localhost:8080`. Register and login go through the Gateway. Auth Service stores users in MySQL database `stayfinder_auth`, hashes passwords with BCrypt, and signs JWTs with an RSA **private** key. Gateway downloads the matching **public** keys from JWKS and rejects missing or invalid tokens with **401**.

```text
POST /api/auth/login
        │
        ▼
API Gateway :8080  →  Auth Service :8081
        │
        ▼
BCrypt match  →  RSA private key signs JWT  →  accessToken
```

```text
GET /api/auth/me
Authorization: Bearer <jwt>
        │
        ▼
Gateway verifies signature via JWKS  →  Auth verifies again  →  200 user
```

## Modules

| Application | Port | Role |
| --- | --- | --- |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Only public HTTP entry; JWT check at the edge |
| `auth-service` | 8081 | Users, login, RSA JWT, JWKS |

## Demo users (seeded on Auth startup)

| Email | Password | Role |
| --- | --- | --- |
| `customer@stayfinder.local` | `Customer@123` | CUSTOMER |
| `admin@stayfinder.local` | `Admin@123` | ADMIN |
| `owner@stayfinder.local` | `Owner@123` | HOTEL_OWNER |

Register always creates **CUSTOMER**. Clients cannot send a role field.

## Run locally

MySQL must be running on `127.0.0.1:3306`. Set `MYSQL_PASSWORD` in the shell if your root user has one. Do not commit `.env`.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Terminal 1 — Eureka first
cd eureka-server && mvn spring-boot:run

# Terminal 2 — Auth (creates stayfinder_auth if needed)
cd auth-service && mvn spring-boot:run

# Terminal 3
cd api-gateway && mvn spring-boot:run
```

- Eureka dashboard: http://localhost:8761 — you should see `API-GATEWAY` and `AUTH-SERVICE`
- Login: `curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"email":"customer@stayfinder.local","password":"Customer@123"}'`
- Expected 401: `curl -i http://localhost:8080/api/auth/me`
- Expected 503: `curl -i http://localhost:8080/api/bookings/hotels`

More requests: [http/phase-2.http](http/phase-2.http)

## Why RSA + JWKS, not a shared secret?

| HMAC shared secret | RSA + JWKS (StayFinder) |
| --- | --- |
| Gateway could mint tokens if it has the secret | Only Auth has the private key |

Restarting Auth Service creates a new in-memory key pair, so old JWTs become invalid. That is a portfolio simplification.

## Docs

- [Architecture](docs/architecture/phase-2-auth-jwt.md)
- [Sequence diagrams](docs/sequence-diagrams/phase-2-jwt-flow.md)
- [Interview notes](docs/interview-notes/phase-2-auth-jwt.md)
- Day 1: [Eureka + Gateway](docs/architecture/phase-1-eureka-gateway.md)

## Later days (not in this repo yet)

Booking, Food + Feign, Kafka, Resilience4j, Docker Compose, React.
