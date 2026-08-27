# StayFinder — Day 5: Docker Compose

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL · Kafka · Docker Compose

StayFinder is a hotel booking and in-room dining platform. **This commit series is Day 5:** the same six Spring apps, plus MySQL and Kafka, start as one stack. The browser still uses `http://localhost:8080`. Inside Docker, services use **DNS names**, not `localhost`. React is not in this tree yet.

## What Day 5 proves

`localhost` inside a container is **that container**. Compose sets:

| Need | Host JVM | Compose |
| --- | --- | --- |
| Eureka | `localhost:8761` | `eureka-server:8761` |
| JWKS | `localhost:8081` | `auth-service:8081` |
| Kafka | `localhost:9092` | `kafka:29092` |
| MySQL | `127.0.0.1:3306` | `mysql:3306` (host publish **3307**) |

There is **no Config Server**. Env vars in `docker-compose.yml` are the config. One MySQL process, three databases (`stayfinder_auth`, `stayfinder_booking`, `stayfinder_food`) — logical database-per-service for the demo.

Do not run `docker-compose.yml` and `docker-compose.kafka.yml` at the same time (both use 9092).

## Run the stack

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./scripts/compose-up.sh
```

- Gateway: http://localhost:8080
- Eureka: http://localhost:8761 — instances should show `auth-service`, `booking-service`, not `localhost`
- Health: [http/phase-7.http](http/phase-7.http)

```bash
docker compose down        # stop
docker compose down -v     # stop and wipe MySQL volume
```

## Docs

- [Architecture](docs/architecture/phase-7-docker-compose.md)
- [Sequence](docs/sequence-diagrams/phase-7-compose-startup.md)
- [Interview notes](docs/interview-notes/phase-7-docker-compose.md)

## Later (not in this repo yet)

React UI (talks only to Gateway `:8080`).
