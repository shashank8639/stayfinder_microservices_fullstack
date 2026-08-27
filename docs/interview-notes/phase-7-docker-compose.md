# Phase 7 Interview Notes — Docker Compose

You should be able to explain why `localhost` is wrong in a container, why Kafka needs two advertised listeners, and why `depends_on` without a healthcheck is not “ready”.

---

## 30-second explanation

StayFinder runs as Docker Compose: MySQL, Kafka, Eureka, Auth, Booking, Food, Notification, Gateway. The browser still hits `localhost:8080`. Inside the network, services use Docker DNS names. Environment variables override Eureka, JWKS, Kafka, and MySQL. There is no Config Server.

## 60-second explanation

A process inside a container that calls `localhost:8761` is calling **itself**, not Eureka on your Mac. Compose sets `EUREKA_CLIENT_SERVICE_URL=http://eureka-server:8761/eureka/` and `STAYFINDER_JWK_SET_URI=http://auth-service:8081/api/auth/.well-known/jwks.json`.

Kafka advertises `kafka:29092` to other containers and `localhost:9092` to the host. One MySQL has three databases — logical isolation for the demo, not three servers. Healthchecks gate startup because a listening PID is not a ready Spring app.

## 2-minute explanation

`application.yml` keeps localhost defaults for `mvn spring-boot:run`. Compose never relies on those defaults for inter-service URLs.

Eureka instances advertise the **compose service name** (`prefer-ip-address=false`) so `lb://booking-service` resolves to `booking-service:8082`.

`depends_on: mysql` without `condition: service_healthy` starts Auth while MySQL is still initializing. Flyway then fails. Healthchecks are how Compose waits; they are not APM.

Config Server is deferred: the only environment-specific values we have are hosts and ports, and Compose already injects them.

---

## Short interview answers

### Why not localhost inside Docker?

**Short interview answer**

`localhost` is the container, not the laptop and not the sibling service.

**Example from StayFinder**

JWKS must be `http://auth-service:8081/api/auth/.well-known/jwks.json`. Booking must not go through Gateway to fetch keys, and it must not use `localhost:8081`.

**Likely follow-up**

How do they find each other? Docker DNS on the compose network, plus Eureka for `lb://` routes.

### Why two Kafka listeners?

**Short interview answer**

Clients use the **advertised** address. Containers need `kafka:29092`. The host needs `localhost:9092`. One advertised name cannot be both.

**Deeper explanation**

The Phase 5 kafka-only file advertises localhost only, which is correct for host JVMs. Full-stack Compose would break producers/consumers inside the network with that file. The two compose files both bind 9092 — do not run them together.

### Why one MySQL with three databases?

**Short interview answer**

Database-per-service is about **ownership of schema**, not necessarily three machines. The demo uses one process and three logical DBs.

**Likely follow-up**

What would production do? Separate servers or catalogs, separate credentials, network isolation. We skipped that cost.

### What does `depends_on` actually wait for?

**Short interview answer**

Without a healthcheck: process started. With `condition: service_healthy`: the healthcheck command succeeded.

**Example from StayFinder**

Auth waits for MySQL `mysqladmin ping`. Gateway waits for Auth/Booking/Food `/actuator/health`. Food does not treat an OPEN Resilience4j circuit as container death.

### Why no Config Server?

**Short interview answer**

Compose env vars already override the hostnames that differ from local YAML. Config Server is another moving part (and usually a Git backend) that this demo does not need yet.

**Deeper explanation**

Config Server shines when many instances share versioned YAML. StayFinder has one of each service on one machine.

### Dockerfile: why copy a jar instead of building in Docker?

**Short interview answer**

The image stays a small JRE. Compilation stays on the host with the same JDK 21 we already use for tests.

**Simplification**

A multi-stage Maven build would make `docker compose up --build` self-contained. We chose `mvn package` then copy, via `./scripts/compose-up.sh`.
