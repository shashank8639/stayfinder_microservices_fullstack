# Phase 2 Architecture — Auth Service + JWT

## What this phase proves

A client can register and log in through the Gateway. Auth Service hashes passwords, signs a JWT with an RSA **private** key, and publishes the matching **public** key as JWKS. Gateway validates the JWT signature before forwarding protected routes. Auth Service validates the JWT again for `/api/auth/me` and `/api/auth/admin-check`.

Booking/Food still do not exist. Public catalog GETs (`/api/bookings/hotels`) are `permitAll`, so they return **503** until Booking exists. A missing JWT on a protected path such as `/api/auth/me` or `POST /api/bookings` returns **401**. A valid JWT on `POST /api/bookings` still returns **503**.

## Why MySQL, not PostgreSQL

StayFinder uses **MySQL** because that is the database already in your practice path. PostgreSQL is a valid alternative in many companies, but it is not mandatory for this portfolio. Interviewers care that **each service owns its own database**, not that you picked Postgres.

Logical isolation in Phase 2:

```text
stayfinder_auth   → auth-service only
```

Later:

```text
stayfinder_booking → booking-service only
stayfinder_food    → food-service only
```

One MySQL **server**, three **databases**, is still database-per-service for a demo. Do not let Food Service query `stayfinder_auth`.

## JWT flow

```text
React / curl
    → POST /api/auth/login  (Gateway, no JWT required)
    → Auth Service
    → BCrypt matches password
    → RSA private key signs JWT
    → client stores accessToken

Client
    → GET /api/auth/me
    → Authorization: Bearer <jwt>
    → Gateway validates signature via JWKS (public key)
    → Auth Service validates JWT again
    → 200 user claims
```

## RSA / JWKS vs shared secret

| Approach | Who can mint tokens? | StayFinder |
| --- | --- | --- |
| HMAC shared secret | Anyone who has the secret, including Gateway | Not used |
| RSA + JWKS | Only Auth Service (private key) | Used |

If Gateway is compromised, attackers can **read** traffic but cannot mint valid StayFinder tokens unless they also steal the Auth private key.

**Simplification:** the RSA key pair is generated in memory at Auth startup. Restarting Auth Service invalidates all outstanding JWTs. Production would store the private key in a secret manager.

## Why Gateway still uses `http://localhost:8081/...jwks.json`

`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` is a normal HTTP URL, not `lb://`. Pointing it at the Gateway (`localhost:8080`) would make Gateway call itself. Phase 2 uses Auth's direct port. Docker Compose can later inject `STAYFINDER_JWK_SET_URI`.

This is a documented simplification, not Eureka routing for JWKS.

## Dual validation

```text
Missing/invalid JWT on a protected path → Gateway 401  (edge)
Valid CUSTOMER JWT + ADMIN endpoint → Auth 403  (authoritative authorization)
Valid JWT + POST /api/bookings           → Gateway OK, then 503 until Phase 3
Public GET /api/bookings/hotels          → 503 until Booking exists (no JWT required)
```

Gateway is the edge. Auth Service is still the authority for its own endpoints. Later, Booking/Food will validate JWT themselves too. Unsigned headers such as `X-User-Id` are not trusted.
