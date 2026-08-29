# StayFinder frontend

Minimal React (Vite) UI for the StayFinder microservices demo.

Talks **only** to the API Gateway:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Do not add Auth, Booking, or Food service URLs.

## Run

Backend must already be up (Compose or `mvn spring-boot:run`). Gateway CORS allows `http://localhost:5173`.

```bash
cp .env.example .env
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

Demo login: `customer@stayfinder.local` / `Customer@123`  
Staff: `admin@stayfinder.local` / `Admin@123`

## JWT (demo)

The access token is stored in `localStorage` (`stayfinder.token`). Axios attaches `Authorization: Bearer …` on every request. A 401 (except login/register) clears storage and the UI sends you to `/login`.

That is **not** a security boundary. Gateway and each service still validate JWT. `ProtectedRoute` only hides pages.

Production would use httpOnly cookies (XSS) and a short TTL. Auth restart still invalidates tokens because the RSA key is in memory.
