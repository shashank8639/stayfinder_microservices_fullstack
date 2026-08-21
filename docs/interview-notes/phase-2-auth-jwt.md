# Phase 2 Interview Notes — Auth + JWT

You should be able to explain register, BCrypt, JWT structure, JWKS, 401 vs 403, and why Gateway and Auth both validate tokens.

---

## 30-second explanation

Auth Service owns users in MySQL database `stayfinder_auth`. Register hashes the password with BCrypt and always assigns CUSTOMER. Login issues an RSA-signed JWT. Gateway is an OAuth2 resource server: it downloads Auth's public JWKS and rejects missing or invalid tokens with 401. Protected Auth endpoints validate the JWT again. A CUSTOMER token on an ADMIN endpoint returns 403.

## 60-second explanation

A JWT has three Base64url parts: header, payload, signature. StayFinder signs with RS256. The payload includes `sub` (user id), `email`, `roles`, `iat`, `exp`, and `iss`. Gateway never uses the private key. It only verifies the signature against JWKS. That is asymmetric crypto: steal the Gateway and you still cannot mint tokens. The RSA key currently lives in Auth memory, so an Auth restart invalidates tokens — a portfolio simplification.

## 2-minute explanation

Authentication answers "who are you?". Authorization answers "what may you do?". Login is authentication. `@PreAuthorize("hasRole('ADMIN')")` is authorization.

Spring Security's `SecurityFilterChain` (servlet, Auth) and `SecurityWebFilterChain` (WebFlux, Gateway) run before controllers. Missing Bearer token hits the AuthenticationEntryPoint → 401. Authenticated but wrong role hits AccessDeniedHandler or method security → 403.

We map JWT claim `roles: ["CUSTOMER"]` to `ROLE_CUSTOMER` because `hasRole('ADMIN')` looks for `ROLE_ADMIN`. If that converter is removed, every `@PreAuthorize("hasRole(...)")` fails even with a valid token.

Register does not accept a role field. Allowing clients to send `"role": "ADMIN"` would be a privilege-escalation bug. Demo ADMIN comes from `DemoUserSeeder`.

Database-per-service: Auth never shares a schema with Booking. MySQL is one server; isolation is by database name.

---

## Short interview answers

### What is JWT?

**Short interview answer**

A compact signed token. The server can verify it without a session store. StayFinder uses RS256.

**Deeper explanation**

Header describes the algorithm and key id. Payload is claims. Signature proves Auth minted it and that it was not altered. Expiration is checked on every request.

**Example from StayFinder**

`JwtService` builds claims and `JwtEncoder` signs. Gateway uses `jwk-set-uri`.

**Likely follow-up**

- Where do you store JWT in a browser?
- What if the token is stolen?

**Strong answer**

For this demo the client holds the access token and sends it as `Authorization: Bearer`. Stolen tokens work until expiry. Production would add short TTL, refresh tokens, rotation, and HTTPS only. Frontend route guards are UX, not security — the API still enforces JWT.

### Why validate JWT at Gateway and at the service?

**Short interview answer**

Gateway is the edge: it stops junk traffic. The service is the authority: another client might bypass Gateway.

**Example from StayFinder**

Gateway requires JWT for `/api/auth/me`. Auth Service also requires JWT for `/me`. Booking will do the same later.

**Likely follow-up**

- Why not trust `X-User-Id` from Gateway?
- mTLS between services?

**Strong answer**

Headers can be spoofed if someone reaches the service port. StayFinder does not treat `X-User-Id` as identity. Service-to-service mTLS is a production improvement, currently out of scope.

### 401 vs 403

**Short interview answer**

401: we do not know who you are (missing/invalid token). 403: we know who you are and you are not allowed.

**Example from StayFinder**

No JWT on `/api/auth/me` → 401. CUSTOMER JWT on `/api/auth/admin-check` → 403.

---

## Important classes

| Class | Why it exists |
| --- | --- |
| `AuthController` | HTTP API: register, login, me, jwks, admin-check |
| `AuthService` | Business rules; never in the controller |
| `JwtService` | Builds claims and asks `JwtEncoder` to sign |
| `RsaKeyConfig` | Creates RSA key, encoder, decoder |
| `SecurityConfig` | Which paths are public; resource server |
| `JwtRoleConverter` | `roles` claim → `ROLE_*` authorities |
| `DemoUserSeeder` | Local ADMIN/CUSTOMER/OWNER; not a public API |
| `GatewaySecurityConfig` | WebFlux JWT at the edge |
| `V1__create_users.sql` | Schema owned by Auth, applied by Flyway |

## Runtime internals

1. `PasswordEncoder.encode` runs BCrypt (slow by design).
2. User row + `user_roles` row persist in `stayfinder_auth`.
3. `NimbusJwtEncoder` signs with the in-memory private key and sets `kid` in the header.
4. Gateway `NimbusJwtDecoder` fetches JWKS, finds the `kid`, verifies RS256, checks `exp`.
5. Converter reads `roles` and creates `ROLE_CUSTOMER`.
6. `@PreAuthorize("hasRole('ADMIN')")` looks for `ROLE_ADMIN`.

## Reverse-engineering drills

1. Remove `@EnableMethodSecurity`. What happens to `/admin-check`?
2. Remove `JwtRoleConverter`. Why does a valid ADMIN token still get 403?
3. Add `role` to `RegisterRequest` and save it. What security bug did you just create?
4. Point Gateway `jwk-set-uri` at `http://localhost:8080/api/auth/.well-known/jwks.json`. Why is that a bad idea?
5. Restart Auth Service, keep an old JWT. Why does Gateway suddenly return 401?
6. Stop Auth Service. Can Gateway still validate tokens it already cached from JWKS? For how long?
7. Why is Flyway `ddl-auto: validate` safer than `update`?

## Common failure scenarios

| Symptom | Cause |
| --- | --- |
| Auth fails to start, Access denied for MySQL | Set `MYSQL_PASSWORD` in the shell; do not commit it |
| Auth fails, Unknown database | Flyway/JDBC `createDatabaseIfNotExist=true` or create `stayfinder_auth` |
| Gateway 401 on login | Login path not permitAll |
| Login 401 with good password | Demo user not seeded, or Auth restarted and you used a JWT not a password |
| 403 on admin-check with admin JWT | Role converter missing `ROLE_` prefix |
| JWKS contains `"d"` | You accidentally published the private key |

## Honesty for interviews

Do not say you built Keycloak or a full OAuth2 authorization server. Say: "Auth Service issues JWTs as a resource-server-compatible issuer with JWKS. It is enough to demonstrate RSA validation. A company might replace this with Keycloak or Spring Authorization Server."
