# Phase 1 Architecture — Eureka + API Gateway

## What this phase proves

A client can reach one public URL (`http://localhost:8080`). The Gateway asks Eureka which instances exist. Because Booking/Food/Auth are not built yet, routed calls fail in a controlled way (503), while Gateway and Eureka themselves stay healthy.

## Components

### Eureka Server

Eureka is a **service registry**.

- **Registration:** a service starts and says "I am `api-gateway` at this host/port".
- **Discovery:** another component asks "where is `booking-service`?"
- **Heartbeat:** each registered instance renews its lease. If heartbeats stop, Eureka eventually evicts the instance.

Without Eureka, Gateway would need hardcoded URLs such as `http://localhost:8082`. That breaks as soon as a port, host or replica count changes.

### API Gateway

The Gateway is the **only public HTTP entry point**.

It does not contain booking/food business logic. It:

- matches a path (`/api/bookings/**`)
- resolves `lb://booking-service` through Eureka + Spring Cloud LoadBalancer
- proxies the HTTP request
- returns the downstream response, or 503 if no instance exists

`lb://` means "load-balanced logical service name", not a DNS hostname.

## Request flow (Phase 1)

```text
curl http://localhost:8080/api/bookings/hotels
        │
        ▼
Gateway Path predicate matches booking-service route
        │
        ▼
ReactiveLoadBalancerClientFilter
        │
        ▼
Eureka client cache: instances of booking-service?
        │
        ├── none → 503 SERVICE_UNAVAILABLE   (current Phase 1 result)
        └── one or more → proxy to that instance (later phases)
```

## Why both components?

| If we only had Eureka | If we only had Gateway |
| --- | --- |
| Clients would still need to know every service URL | We would hardcode `http://localhost:8081` and lose dynamic discovery |

Together they give: **stable public URLs + dynamic internal locations**.

## Ports

| Application | Port | Why |
| --- | --- | --- |
| Eureka | 8761 | Conventional Eureka port; interviewers recognise it |
| Gateway | 8080 | The public port React will use later |

## What we intentionally did not add

- Config Server — extra moving part before anything runs
- Spring Security — no Auth Service yet
- Databases — no business services yet
- Docker Compose — Phase 7
- React — it would have nothing real to call except health
