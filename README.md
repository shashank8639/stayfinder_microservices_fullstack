# StayFinder — Day 6: React on the Gateway

Java 21 · Spring Boot 3.5.4 · Spring Cloud 2025.0.3 · MySQL · Kafka · Docker Compose · React (Vite)

StayFinder is a hotel booking and in-room dining platform. **This commit series is Day 6:** a Vite React app on `:5173` that calls **only** `http://localhost:8080`. It never uses Auth `:8081`, Booking `:8082`, Food `:8083`, Eureka, or Kafka.

## What Day 6 proves

```text
React :5173
        │
        ▼  VITE_API_BASE_URL=http://localhost:8080
API Gateway :8080
        │
        ▼  lb:// via Eureka
Auth / Booking / Food
```

`ProtectedRoute` only hides pages. JWT is still checked at the Gateway and in each service. The token is in `localStorage` for this demo (XSS-vulnerable; production would use httpOnly cookies).

## Run

Start the backend (Compose or `mvn spring-boot:run`), then:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173

| Email | Password | Role |
| --- | --- | --- |
| `customer@stayfinder.local` | `Customer@123` | Guest |
| `admin@stayfinder.local` | `Admin@123` | Staff |

## Docs

- [Architecture](docs/architecture/phase-9-react.md)
- [Sequence](docs/sequence-diagrams/phase-9-react-flow.md)
- [Interview notes](docs/interview-notes/phase-9-react.md)
- Frontend: [frontend/README.md](frontend/README.md)
