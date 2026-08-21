# Phase 1 Interview Notes — Eureka + API Gateway

After Phase 1 you should be able to draw and explain the two processes, the two ports, and what 503 means.

---

## 30-second explanation

StayFinder currently has two Spring Boot applications. Eureka on port 8761 is the service registry. The API Gateway on port 8080 is the only public entry point. The Gateway registers with Eureka and will later look up `booking-service` and `food-service` by name instead of hardcoded localhost ports.

## 60-second explanation

A request never goes to Eureka for business data. The client calls Gateway. Gateway matches `/api/bookings/**` to a route whose URI is `lb://booking-service`. The `lb` prefix means Spring Cloud LoadBalancer should ask the discovery client for instances. Eureka stores those instances because each service registers on startup and sends heartbeats. In Phase 1 those business services do not exist, so the Gateway returns 503. That is the correct failure, not a broken Gateway.

## 2-minute explanation

Microservices here means independent processes with independent future databases, not a package-per-feature monolith. Service discovery solves the problem of changing hosts, ports and replica counts. An API Gateway solves the problem of giving the frontend one URL, one CORS policy and later one JWT check at the edge.

Eureka Server is started with `@EnableEurekaServer` and is configured **not** to register with itself. Gateway uses the Eureka **client** starter, `@EnableDiscoveryClient`, and explicit YAML routes. We did not enable the discovery locator, because that would expose internal service IDs such as `/BOOKING-SERVICE/**` to clients.

Gateway is WebFlux/Netty, not Tomcat. That is why `spring-boot-starter-web` is absent from the Gateway module. Eureka Server is a servlet application. Mixing `starter-web` into Gateway is a common failure.

Authorization headers are forwarded by default. We still will not trust unsigned custom headers later; each service will validate JWT itself. That is Phase 2+.

---

## Short interview answers

### What is Eureka?

**Short interview answer**

Eureka is a service registry. Services register their name and location, send heartbeats, and other components look them up by logical name.

**Deeper explanation**

Registration is "I exist". Discovery is "where are the instances of this name?". The registry is eventually consistent. Clients typically cache the instance list and refresh it.

**Example from StayFinder**

`api-gateway` registers as `api-gateway`. Later `booking-service` will register as `booking-service`. Gateway routes use `lb://booking-service`.

**Likely follow-up**

- What happens if Eureka itself is down?
- Difference between client-side and server-side discovery?

**Strong answer**

If Eureka is briefly down, already-running clients can continue using their last cached instances. New services cannot register. StayFinder currently has one Eureka node, which is a portfolio simplification; production would use replicated peers or platform DNS such as Kubernetes.

### What is an API Gateway?

**Short interview answer**

It is a reverse proxy that is the single public entry point. It routes by path, applies cross-cutting concerns, and hides internal service URLs.

**Deeper explanation**

Routing is implemented as a filter chain: predicate match, then filters, then proxy. StayFinder uses Spring Cloud Gateway, which is reactive.

**Example from StayFinder**

`/api/bookings/**` → `lb://booking-service`. React will only know `http://localhost:8080`.

**Likely follow-up**

- Why not Nginx only?
- Why not put business logic in the Gateway?

**Strong answer**

Nginx can route too. We use Spring Cloud Gateway because it integrates with Eureka, later JWT, and Spring configuration in the same stack. Business rules stay in the services so the Gateway remains replaceable.

### Why not call services directly from the frontend?

**Short interview answer**

That would expose internal hosts, multiply CORS/JWT configuration, and couple the UI to every service port.

**Example from StayFinder**

There is one intended frontend env var: `VITE_API_BASE_URL=http://localhost:8080`. Individual service URLs must not appear in React.

---

## Important annotations

| Annotation | Where | What it actually does |
| --- | --- | --- |
| `@SpringBootApplication` | both apps | Component scan + auto-configuration + property support |
| `@EnableEurekaServer` | Eureka | Imports Eureka server configuration and registry endpoints (`/eureka/**`, dashboard) |
| `@EnableDiscoveryClient` | Gateway | Marks the app as a discovery client so it registers and can resolve names |

`@EnableDiscoveryClient` is not strictly required when the Eureka client is on the classpath, because Spring Cloud auto-configures it. We keep it because it makes the intent obvious in interviews.

## Important configuration

| Property | Meaning |
| --- | --- |
| `eureka.client.register-with-eureka: false` | Eureka Server is not a client of itself |
| `eureka.client.fetch-registry: false` | Eureka Server does not download a registry |
| `eureka.server.enable-self-preservation: false` | Local demo only; expire dead instances more eagerly |
| `spring.cloud.gateway.server.webflux.routes` | Current Gateway 4.3 property prefix (not the old `spring.cloud.gateway.routes`) |
| `uri: lb://booking-service` | Discover by Eureka service ID, then load-balance |
| `discovery.locator.enabled: false` | Do not auto-expose `/service-id/**` routes |

## Runtime internals worth remembering

1. Gateway starts Netty on 8080.
2. Eureka client registers `api-gateway` with `http://localhost:8761/eureka/`.
3. A request hits Gateway's `RoutePredicateHandlerMapping`.
4. The matching `Route` has `uri=lb://booking-service`.
5. `ReactiveLoadBalancerClientFilter` asks the load balancer for an instance.
6. The load balancer asks the discovery client (Eureka cache).
7. No instance → 503. Later, an instance → HTTP proxy, same path, most headers copied.

## Reverse-engineering drills

1. Remove `@EnableEurekaServer`. What UI/endpoints disappear? Why does Gateway then fail to register?
2. Set Gateway `register-with-eureka: false`. What changes on the Eureka dashboard? Does routing still work later?
3. Change `lb://booking-service` to `http://localhost:8082`. What did you just destroy?
4. Enable `discovery.locator.enabled: true`. What extra routes appear?
5. Add `spring-boot-starter-web` to the Gateway pom. What class of error do you expect?
6. Start Gateway before Eureka. What do the logs say, and does it recover?
7. Stop Gateway. How long until Eureka shows it as down, and which properties affect that?

## Eureka dashboard vs actuator health

The Eureka **dashboard** and `/eureka/apps` are the source of truth for registration.

Gateway `/actuator/health` may still print `Eureka discovery client has not yet successfully connected` even after a successful register (HTTP 204). That is an actuator-indicator quirk, not a failed registration. Confirm with the dashboard: you should see `API-GATEWAY` UP.

## Common failure scenarios

| Symptom | Likely cause |
| --- | --- |
| Gateway cannot start, connection refused 8761 | Eureka not running |
| Eureka dashboard empty | Gateway failed to register; check `spring.application.name` and `defaultZone` |
| `503` on `/api/bookings/hotels` | Expected in Phase 1 |
| Routes actuator 404 | `gateway` endpoint not exposed or access restricted |
| CORS error later from React | origin not allowed; Gateway is the place to fix it |
| Bean / Netty vs Tomcat conflict | `starter-web` added to Gateway |

## Honesty for interviews

Do not say "I built a production service mesh". Say: "This is a two-process skeleton that demonstrates discovery and edge routing. Business services, JWT, Kafka and React come in later phases."
