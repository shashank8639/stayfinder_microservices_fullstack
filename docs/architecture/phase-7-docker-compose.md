# Phase 7 Architecture — Docker Compose

## What this phase proves

The same six Spring processes, plus MySQL and Kafka, can start as **one stack**. The laptop still talks to Gateway `:8080` and Eureka `:8761`. Inside the Docker network, services must **not** use `localhost`.

```text
Host (browser / curl / .http)
        │
        ▼ published ports
  api-gateway :8080
  eureka-server :8761
  auth :8081  booking :8082  food :8083  notification :8084
  kafka :9092   mysql :3307 → container 3306
        │
        ▼ Docker DNS (not localhost)
  mysql, kafka:29092, eureka-server, auth-service, ...
```

There is **no Config Server**. Environment variables in `docker-compose.yml` are the config for this phase.

## Why localhost breaks in a container

`localhost` in Auth means Auth itself. If Booking's JWKS URI stays `http://localhost:8081/...`, Booking looks for JWKS inside the Booking container and JWT validation dies.

Same trap for:

| Setting | Host JVM | Compose |
| --- | --- | --- |
| Eureka defaultZone | `http://localhost:8761/eureka/` | `http://eureka-server:8761/eureka/` |
| JWKS | `http://localhost:8081/api/auth/.well-known/jwks.json` | `http://auth-service:8081/api/auth/.well-known/jwks.json` |
| Kafka | `localhost:9092` | `kafka:29092` |
| MySQL | `127.0.0.1:3306` | `mysql:3306` |

YAML defaults stay on localhost so `mvn spring-boot:run` still works. Compose **overrides** the env vars.

## Eureka advertise the service name, not a container IP

On the laptop, `prefer-ip-address: true` is useful (other host processes call `127.0.0.1`). In Compose we set:

- `EUREKA_INSTANCE_HOSTNAME=auth-service` (and the same for each app)
- `EUREKA_INSTANCE_PREFER_IP_ADDRESS=false`

Gateway then calls `http://auth-service:8081`, which Docker DNS resolves. Advertising `172.x.x.x` often works on a bridge network and is harder to read in the Eureka UI.

## Kafka dual listeners

The Phase 5 file `docker-compose.kafka.yml` advertises **only** `localhost:9092`. That is correct for host JVMs and **wrong** for containers (they would be told to connect to their own localhost).

Full-stack Kafka has two advertised listeners:

| Listener | Advertised | Who uses it |
| --- | --- | --- |
| `PLAINTEXT` | `kafka:29092` | Booking, Food, Notification in Compose |
| `PLAINTEXT_HOST` | `localhost:9092` | optional host tools |

Do not run both compose files at once. Both publish `9092`.

## One MySQL, three databases

`docker/mysql/init/01-databases.sql` creates `stayfinder_auth`, `stayfinder_booking`, `stayfinder_food` in **one** MySQL 8 process.

Interview line: this is **logical** database-per-service for a demo. Three MySQL servers would be closer to production isolation and is more than this portfolio needs.

Host MySQL already occupies `3306`, so Compose publishes **`3307:3306`**. Containers still use port `3306` on host `mysql`.

## Healthchecks vs `depends_on`

`depends_on` without a healthcheck only waits until the **process starts**. MySQL accepting connections, Kafka electing a controller, and a JVM finishing Flyway all happen later.

Compose uses `condition: service_healthy` so Auth does not start until MySQL answers `mysqladmin ping`, and Gateway waits until Auth/Booking/Food report `/actuator/health`.

That is **orchestration**, not observability. There is still no Prometheus/Grafana.

Food's circuit-breaker health contributor is **off** in Compose (`MANAGEMENT_HEALTH_CIRCUITBREAKERS_ENABLED=false`). An OPEN circuit means Booking is sick, not that the Food process should be marked dead.

## Images

`docker/app.Dockerfile` is a **JRE** image. It copies a jar already built on the host:

```bash
mvn -DskipTests package
docker compose up --build
```

`./scripts/compose-up.sh` runs both. Tests stay on the host (`mvn test`); they use H2 / Embedded Kafka and do not need Docker.

## Why not Config Server?

Spring Cloud Config would be another process that serves YAML from Git. Compose already injects the values that **must** change between laptop and Docker (hosts, JWKS, Kafka). Adding Config Server now is another thing that can be down, plus a Git backend we do not need for a single demo machine.

Interview: know what Config Server is; this repo defers it on purpose.

## Simplifications

- One MySQL container, three schemas
- Demo root password default `stayfinder` when `MYSQL_PASSWORD` is unset
- No Config Server
- No Kubernetes / Swarm
- Thin jar copy, not a Maven build inside Docker
- Healthchecks, not a metrics stack
