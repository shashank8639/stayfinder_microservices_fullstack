# Phase 8 — Interview question bank

Timed story first: [phase-8-walkthrough.md](phase-8-walkthrough.md). Depth: phase 1–7 notes.

Answers are 30–60 seconds. Starred items are common Dubai Java/microservices follow-ups.

---

## A. Microservices (30)

### 1. What is a microservice?
**A.** An independently deployable process with its own data and failure mode, talking over the network. *StayFinder:* Booking and Food are separate Boot apps, not packages in one WAR.

### 2. Why not a monolith for this hotel demo?
**A.** A monolith would be enough for the product. We split to **demonstrate** discovery, DB-per-service, Feign, Kafka, and isolation. Say that out loud.

### 3. What is service discovery?
**A.** Services register a name and location; callers look up by name, not hardcoded hosts. *StayFinder:* Eureka + `lb://booking-service`.

### 4. Client-side vs server-side discovery?
**A.** Client-side: the caller (Gateway) asks the registry and load-balances. Server-side: a proxy/mesh does it. Eureka + Spring Cloud LoadBalancer is client-side.

### 5. What if Eureka is down?
**A.** Already-running clients use their last cache. New instances cannot register; new Gateway processes cannot fetch. We have **one** Eureka node — a portfolio simplification.

### 6. What is an API Gateway?
**A.** Single public entry: routing, CORS, auth at the edge. *StayFinder:* `:8080` only. No business rules like overlap.

### 7. Why not call services from the frontend?
**A.** Exposes internal hosts, multiplies CORS/JWT, couples UI to every port. React must use one base URL.

### 8. Why explicit routes, not discovery locator?
**A.** Locator would expose `/BOOKING-SERVICE/**`. We want stable `/api/bookings/**`.

### 9. What does `lb://` mean?
**A.** Load-balanced URI: resolve the service id from discovery, pick an instance, proxy.

### 10. Database-per-service?
**A.** Each service owns its schema. No JOINs across Auth/Booking/Food. IDs may be copied (`user_id`, `bookingId`).

### 11. Same MySQL server — is that cheating?
**A.** Logical isolation for a demo. Production: separate servers/credentials/networks. Interviewers care that Food does not query Booking tables.

### 12. Sync vs async communication?
**A.** Sync: caller needs the answer now (Feign GET booking). Async: side effect can lag (email on Kafka).

### 13. Why not saga for booking + food?
**A.** Food is not a second step of booking. It is a later command that **reads** booking state. No distributed write spanning both DBs.

### 14. How do you handle a distributed transaction?
**A.** We don’t. Each service commits locally. Cross-service consistency is application-level (Feign check, copied ids). 2PC across microservices is what we avoid.

### 15. Idempotency?
**A.** Retrying a GET is safe. Retrying “charge card” is not. Food retries only the booking **read**. Confirm booking is not retried by a gateway filter.

### 16. How would you scale Booking?
**A.** More instances behind Eureka; Gateway load-balances. Watch the room **pessimistic lock** — it is per-database, so all instances must hit the same DB. Kafka partitions if consumers scale.

### 17. How would you secure service-to-service calls?
**A.** We relay the user JWT (simple). Production: mTLS, or a token exchange so Food has its own identity. I did not implement mTLS.

### 18. API versioning?
**A.** Not implemented. I would version at the Gateway path (`/api/v1`) or headers if Booking and Food must evolve independently.

### 19. Shared library of entities?
**A.** Dangerous — couples deployability. We share nothing but duplicated **event records** and similar DTO shapes. `BookingView` is Food’s read model, not Booking’s JPA entity.

### 20. What is a bounded context?
**A.** A model that makes sense inside one service. “Booking” in Food is an id + status, not a Hibernate `Booking` graph.

### 21. Observability vs healthchecks?
**A.** Compose healthchecks are “process ready”. They are not tracing/metrics. Actuator health is exposed locally; production would scrape Micrometer and not leave `circuitbreakers` public.

### 22. What happens when Booking goes down?
**A.** Catalog/booking HTTP fail. Food orders **503** (fail closed). Already-confirmed bookings still exist. Kafka/Notification unrelated. Gateway returns 503 for `/api/bookings/**`.

### 23. What happens when Notification goes down?
**A.** Booking confirm still 200. Messages wait on the topic. Emails lag. That is the point of the broker.

### 24. What happens when Auth goes down?
**A.** New logins fail. Existing JWTs still validate until expiry **if** JWKS is cached; our in-memory key means Auth **restart** also invalidates tokens.

### 25. Why no Payment service?
**A.** Mock confirm is a status flag for the demo. A real PSP would be a separate bounded context, webhooks, and idempotent payment records. I would not call this a payments platform.

### 26. Why is Gateway WebFlux?
**A.** Spring Cloud Gateway is reactive (Netty). Adding `starter-web` (Tomcat) on the same app is a classic broken setup.

### 27. CORS on Gateway vs services?
**A.** Browser talks only to Gateway, so CORS lives there (`localhost:*`). Services don’t need to please the browser.

### 28. Eventually consistent — example?
**A.** Auth deletes a user; Booking still has `user_id`. Notification may lag seconds after CONFIRMED. Food’s `bookingId` is a copy, not a live FK.

### 29. How would you split the monorepo later?
**A.** One repo is for learning and one BOM. Production teams often split artifacts and CI. The **runtime** boundaries already exist (separate processes/DBs).

### 30. When would you *not* use microservices?
**A.** One team, one deploy, unclear domains, no isolation need. Start modular monolith; split when deploy/scale/ownership demands it.

---

## B. Spring Boot (20)

### 1. What is Spring Boot?
**A.** Opinionated Spring: auto-config, embedded server, starters, Actuator. *StayFinder:* 3.5.4, parent POM.

### 2. What is a Spring Cloud BOM?
**A.** A bill of materials that aligns Cloud library versions to a Boot generation. We use **2025.0.3** (Boot 3.5). **2025.1.x is Boot 4 — do not mix.**

### 3. `@SpringBootApplication`?
**A.** `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

### 4. `application.yml` vs env vars?
**A.** YAML is defaults. Env overrides (`MYSQL_HOST`, `STAYFINDER_JWK_SET_URI`). Compose is our config story.

### 5. Why Flyway + `ddl-auto: validate`?
**A.** Schema is owned by migrations. Hibernate must not invent columns. Tests use H2 with the same idea.

### 6. Why `open-in-view: false`?
**A.** Avoid lazy-load in the view/filter after the service method. Fetch what you need in the transaction (JOIN FETCH on booking+room+hotel).

### 7. Record vs Lombok DTO?
**A.** Records are immutable API/event shapes. We use them for DTOs; entities stay classes.

### 8. `@RestController` vs `@Controller`?
**A.** RestController = `@Controller` + `@ResponseBody`. JSON APIs.

### 9. Validation?
**A.** `jakarta.validation` on request records (`@NotNull` dates, `@Email`). Failures → 400 via `MethodArgumentNotValidException`.

### 10. Global exception handler?
**A.** `@RestControllerAdvice` maps domain exceptions to 400/401/403/409/503. Keep controllers thin.

### 11. Profiles?
**A.** Tests use `src/test/resources/application.yml` (H2, Kafka auto-config excluded where needed). No `prod` profile yet — Compose uses env.

### 12. Actuator?
**A.** `/actuator/health` for Compose and demos. Food also exposes `circuitbreakers`. Do not treat that as a public product API.

### 13. Embedded vs external Tomcat?
**A.** Boot embeds Tomcat in servlet apps. Gateway uses Netty. Eureka is servlet.

### 14. Parent POM vs BOM import?
**A.** Parent: Boot plugin + dependency management. We still **import** `spring-cloud-dependencies` in `dependencyManagement`.

### 15. Why exclude Kafka auto-config in Booking/Food API tests?
**A.** Those tests are HTTP/JPA. A real broker would make them slow/flaky. Publishers are `@MockitoBean`. Notification tests use `@EmbeddedKafka`.

### 16. `@Transactional` boundary?
**A.** Booking create/confirm run in a service transaction. Kafka publish is registered `afterCommit` so a rollback does not email.

### 17. Constructor injection?
**A.** Preferred; testable; required deps fail fast. StayFinder services inject repositories/clients via constructors.

### 18. What is a starter?
**A.** A dependency that pulls a stack and auto-config (e.g. `oauth2-resource-server`, `data-jpa`).

### 19. Graceful shutdown?
**A.** Not customized. Production: `server.shutdown=graceful` so in-flight requests finish.

### 20. Why Java 21 explicitly?
**A.** Boot 3.5 wants 17+. This machine’s Homebrew default was JDK 26 — wrong. `JAVA_HOME` must be 21.

---

## C. Spring Security / JWT (15)

### 1. What is JWT?
**A.** A signed token (header.payload.signature). We use **RSA** (asymmetric), not HMAC. Auth signs; others verify with JWKS.

### 2. Why RSA not HS256?
**A.** HS256 shares a secret with every service — any compromised service can forge tokens. RSA: private key only in Auth.

### 3. What is JWKS?
**A.** JSON Web Key Set — public keys at `/.well-known/jwks.json`. Resource servers fetch and cache them.

### 4. Why validate JWT at Gateway *and* service?
**A.** Gateway can be skipped (port-forward, misconfig). Each service is its own resource server. Defense in depth.

### 5. Why JWKS URI hits Auth port, not Gateway?
**A.** Avoid Gateway→Auth→JWKS through Gateway (loop / extra hop). Services need a stable key URL.

### 6. 401 vs 403?
**A.** 401: not authenticated. 403: authenticated but not allowed (role or ownership).

### 7. How do roles get into `hasRole('ADMIN')`?
**A.** Claim `roles` → `JwtRoleConverter` prefixes `ROLE_`. `@PreAuthorize("hasRole('ADMIN')")` looks for `ROLE_ADMIN`.

### 8. Why not look up roles in the DB on every request?
**A.** Stateless. Revocation is weak (we rely on short TTL / restart). Production: short TTL + denylist or introspection.

### 9. Password storage?
**A.** BCrypt. Never log raw passwords. Register copies only email+hash.

### 10. Why can’t register set ADMIN?
**A.** Privilege escalation. Seeded admin exists for demo; promotion would be an admin-only use case we did not build.

### 11. CSRF disabled?
**A.** Bearer tokens from SPA/curl, not cookie session. CSRF matters for cookie auth. Gateway/services disable CSRF.

### 12. Relaying JWT on Feign?
**A.** `RequestInterceptor` copies `Authorization` from `RequestContextHolder`. Missing interceptor → Booking 401 on Feign.

### 13. In-memory RSA limitation?
**A.** New key pair every Auth start. Old tokens fail signature. Production: file/HSM/IdP.

### 14. `sub` claim?
**A.** User id. Booking stores `user_id` from `sub`, not by querying Auth’s users table.

### 15. OPTIONS permitAll?
**A.** CORS preflight has no JWT. Gateway allows OPTIONS so browsers can call later.

---

## D. Kafka (15)

### 1. Why Kafka here?
**A.** Decouple “booking confirmed” from “send email”. Producer should not fail if Notification is down.

### 2. Topic vs partition vs offset vs consumer group?
**A.** Topic = log name. Partition = ordered slice. Offset = position. Group = competing consumers sharing work.

### 3. Our partition count?
**A.** Effectively one (demo). Two Notification instances in one group: only one gets each message.

### 4. At-least-once vs at-most-once vs exactly-once?
**A.** We are at-least-once: commit offset after the listener returns. Crash after log before ack → duplicate email log. Exactly-once needs idempotency or transactions.

### 5. What if the consumer crashes after processing before ack?
**A.** Redelivery. Handler must be safe to run twice (logging twice is OK; charging twice is not).

### 6. Poison message?
**A.** We catch JSON errors, log, still ack so the partition does not stall. Production: DLT.

### 7. Why JSON String not `JsonSerializer` with type headers?
**A.** Avoid Java type headers coupling producer/consumer class names. Both sides deserialize known records.

### 8. Why duplicate event classes?
**A.** Independent deploy. Shared JAR means Food cannot change without Notification. Schema Registry is the grown-up version.

### 9. Why publish after commit?
**A.** Otherwise: email, then rollback, guest thinks they have a room. Dual-write remains: commit then Kafka down → lost email.

### 10. What is an outbox?
**A.** Write event row in the **same** DB transaction as the business row; a relay publishes to Kafka. We did **not** implement it.

### 11. Why disable Kafka health on Booking/Food?
**A.** A broker blip should not mark Booking DOWN — HTTP confirm can still work (and log send failure). Notification **should** be DOWN without Kafka.

### 12. KRaft vs ZooKeeper?
**A.** KRaft: Kafka is its own controller. Our compose uses KRaft, no ZK.

### 13. Advertised listeners?
**A.** Clients connect to what the broker **advertises**. Containers need `kafka:29092`; the laptop needs `localhost:9092`.

### 14. `acks: all`?
**A.** Producer waits for in-sync replicas. With one broker that is “this one node”. Still documents the intent.

### 15. Why not Feign to Notification?
**A.** Would add Notification’s uptime and latency to checkout. Wrong tool for a side effect.

---

## E. Resilience4j (10)

### 1. Retry vs timeout vs circuit breaker?
**A.** Timeout: one attempt’s max time (Feign 2s/3s). Retry: how many attempts. Circuit: stop calling a dead dependency and fail fast.

### 2. Why retry GET, not POST pay?
**A.** GET booking is idempotent. POST pay can double-charge.

### 3. Why ignore 4xx?
**A.** 404 “booking missing” is a business answer, not an outage. Counting it would open the circuit with healthy Booking.

### 4. Why fallback 503 not a stub booking?
**A.** Fail closed. A fake CONFIRMED `BookingView` would place room service for a guest who may not have a room.

### 5. CLOSED / OPEN / HALF_OPEN?
**A.** CLOSED: calls pass, failures counted. OPEN: skip Feign, fallback. After wait, HALF_OPEN probes; success closes, failure opens.

### 6. Why wrapper bean not Feign interface?
**A.** Clear AOP target. Also we **disabled** `openfeign.circuitbreaker` so TimeLimiter does not hop threads and drop JWT.

### 7. Aspect order?
**A.** Circuit outer (order 1), Retry inner (order 2). One user request with 3 attempts = one circuit failure. When OPEN, Retry does not run.

### 8. Why fallback still runs for ignored 4xx?
**A.** `@CircuitBreaker(fallbackMethod=…)` still invokes fallback on those exceptions. We **rethrow** 4xx so Food can map 404/403. `ignoreExceptions` still stops the circuit from opening.

### 9. In-memory circuit state?
**A.** Per Food JVM. Two Food instances have two circuits. Production might use a shared store; often instance-local is enough.

### 10. How do you demo it?
**A.** Stop Booking, fire several food POSTs, watch logs `CLOSED -> OPEN`, actuator OPEN, later 503 with no Feign. Start Booking, wait 10s, success → CLOSED.

---

## F. Docker (10)

### 1. Why Docker Compose?
**A.** One command for MySQL, Kafka, and six JVMs with the **right DNS names**.

### 2. Why is localhost wrong in a container?
**A.** It is that container. JWKS at `localhost:8081` from Booking never reaches Auth.

### 3. `depends_on` without healthcheck?
**A.** Waits for process start, not ready. Auth would Flyway against a MySQL that is still booting.

### 4. Why MySQL on 3307?
**A.** Host already uses 3306. Map `3307:3306`. Inside the network, apps still use `mysql:3306`.

### 5. Dual Kafka listeners?
**A.** Advertised address must match the client’s network. Host vs Docker DNS cannot share one name.

### 6. Why copy jars, not Maven in Docker?
**A.** Thin JRE image; compile with the same JDK 21 as tests. `./scripts/compose-up.sh` packages then `up --build`.

### 7. Why no Config Server in Compose?
**A.** Only hosts change. Env vars already override Spring properties. Config Server is another failure domain.

### 8. Healthcheck vs restart policy?
**A.** Unhealthy ≠ auto-restart unless configured. We use health for **startup order**. OPEN circuit must not kill the Food container (circuit health contributor disabled in Compose).

### 9. `.env` vs compose `environment:`?
**A.** `.env` interpolates the compose file (`MYSQL_PASSWORD`). We do **not** `env_file` into apps, so localhost values in `.env` cannot leak into containers.

### 10. Kafka-only file vs full stack?
**A.** `docker-compose.kafka.yml` is for host `mvn spring-boot:run`. It advertises localhost only. Full stack needs dual listeners. Both bind 9092 — don’t run together.

---

## G. Database / JPA (10)

### 1. Entity vs DTO vs event?
**A.** Entity: persistence. DTO: HTTP. Event: Kafka JSON. Never serialize a lazy Hibernate graph to Kafka.

### 2. Overlap predicate?
**A.** Ranges overlap if `checkIn < existingCheckOut AND checkOut > existingCheckIn`. Ignore CANCELLED.

### 3. Why 409 not 400 for overlap?
**A.** Request is well-formed; it **conflicts** with current state.

### 4. Pessimistic lock?
**A.** `PESSIMISTIC_WRITE` on the room (JOIN FETCH hotel) so two overlapping inserts cannot both pass the count. Not a distributed lock across services.

### 5. Why `TINYINT(1)` for food `available`?
**A.** MySQL boolean vs H2. Column definition keeps both dialects honest.

### 6. Flyway on first Compose boot?
**A.** Empty volume → init SQL creates DBs → Auth/Booking/Food run migrations. `down -v` wipes that.

### 7. `user_id` without FK to Auth?
**A.** Cross-DB FK would break isolation. We copy JWT `sub`. Orphan history if Auth deletes the user.

### 8. Optimistic vs pessimistic?
**A.** Optimistic: version column, retry on conflict — good for low contention. We used pessimistic on a hot room row during insert.

### 9. N+1?
**A.** Confirm path JOIN FETCHes hotel/room so the after-commit event can read names without a closed session.

### 10. Indexes you would add?
**A.** Bookings: `(room_id, check_in, check_out, status)`. Food orders: `(user_id)`, `(booking_id)`. Not all are in the demo migrations — say you would add them.

---

## H. StayFinder-specific (10)

### 1. Draw the system in 60 seconds.
**A.** Client → Gateway → Auth / Booking / Food. Food Feign to Booking. Booking & Food → Kafka → Notification. Three MySQL DBs. Eureka for names.

### 2. Why must a food order include `bookingId`?
**A.** Forces the Feign path. Walk-in dining is out of scope on purpose.

### 3. What did we *not* put on Kafka?
**A.** PENDING create, food status PREPARING/DELIVERED, JWT validation, overlap checks.

### 4. Why Notification has no Gateway route?
**A.** No public email API. If it had one, clients would bypass the async design.

### 5. Seeded users/hotel?
**A.** `customer@` / `Customer@123`, admin, owner. Hotel Grand Horizon Hyderabad, rooms 101/201. Seeder flags in YAML.

### 6. Gateway 503 on `/api/bookings/**` in Phase 1 meant?
**A.** Route matched, no instance in Eureka. Correct failure, not a broken Gateway.

### 7. What breaks if you add `starter-web` to Gateway?
**A.** Two stacks (Tomcat vs Netty). Classic Spring Cloud Gateway failure.

### 8. Resilience4j 2.3.0 vs BOM 2.2.0?
**A.** Mixing versions caused `NoClassDefFoundError: RxJava3FallbackDecorator`. Align with the Cloud BOM (2.2.0).

### 9. What would you say you are strongest at in this repo?
**A.** Pick one and go deep: dual JWT, Feign+JWT+CB without TimeLimiter, after-commit Kafka, or Docker DNS. Do not list all eight services.

### 10. What will you build next if asked?
**A.** React on Gateway only, or outbox, or persistent keys — and you can explain the design **before** coding. Do not promise a week of Kubernetes if you have not run it.

---

## Likely follow-ups (keep ready)

- How would you prevent duplicate food orders from double-clicks? (idempotency key)
- How would you rotate RSA keys? (JWKS multiple keys, kid)
- How would you test Feign in CI? (WireMock / `@SpringBootTest` with mock bean — we mock `BookingClient`)
- Why Northfields not Oakwood? (Boot 3.5 vs Boot 4)
