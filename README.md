# StayFinder — Day 1: Eureka + API Gateway

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3

StayFinder will become a hotel booking and in-room dining platform. **This commit series is Day 1 only:** a service registry and one public HTTP entry point. Auth, Booking, Food, Kafka, and React are not in this tree yet.

## What Day 1 proves

A client talks to **one URL**: `http://localhost:8080`. The Gateway registers with Eureka and will later resolve `lb://booking-service` by name, not `localhost:8082`.

Booking and Food do not exist yet, so routed calls return **503**. That is the correct Phase 1 result — the Gateway is healthy; no instance is registered for those names.

```text
curl http://localhost:8080/api/bookings/hotels
        │
        ▼
API Gateway :8080
        │
        ▼
Eureka :8761  →  no booking-service instance
        │
        ▼
503 SERVICE_UNAVAILABLE
```

## Modules

| Application | Port | Role |
| --- | --- | --- |
| `eureka-server` | 8761 | Service registry (does not register with itself) |
| `api-gateway` | 8080 | Only public HTTP entry; WebFlux, not Tomcat |

Routes are already declared (`/api/auth/**`, `/api/bookings/**`, `/api/food/**`) so later services plug in without changing the public URL. Until those apps exist, those paths 503.

## Run locally

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Terminal 1 — start Eureka first
cd eureka-server && mvn spring-boot:run

# Terminal 2
cd api-gateway && mvn spring-boot:run
```

- Eureka dashboard: http://localhost:8761 — you should see `API-GATEWAY`
- Gateway health: http://localhost:8080/actuator/health
- Expected 503: `curl -i http://localhost:8080/api/bookings/hotels`

## Why both?

| Only Eureka | Only Gateway |
| --- | --- |
| Clients still need every service URL | You hardcode `localhost:8081` and lose discovery |

Together: **stable public paths + dynamic internal locations**.

## Docs

- [Architecture](docs/architecture/phase-1-eureka-gateway.md)
- [Sequence diagrams](docs/sequence-diagrams/phase-1-request-flow.md)
- [Interview notes](docs/interview-notes/phase-1-eureka-gateway.md)

## Later days (not in this repo yet)

Auth + JWT, Booking, Food + Feign, Kafka, Resilience4j, Docker Compose, React.
