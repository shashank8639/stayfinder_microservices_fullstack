# Phase 9 Interview Notes — React + Gateway

You should be able to explain why React has one base URL, where the JWT lives, and why `ProtectedRoute` is not authorization.

---

## 30-second explanation

StayFinder’s UI is a Vite React app on port 5173. It calls only `http://localhost:8080`. Login stores a JWT in localStorage. Axios attaches it. Hotels are public. Bookings and food orders need that header. The Gateway and each service still validate the token.

## 60-second explanation

`VITE_API_BASE_URL` is the Gateway, not Auth or Booking. Putting service URLs in the frontend would bypass discovery and multiply CORS.

`ProtectedRoute` redirects guests to `/login`. That is UX. A CUSTOMER can still hit `/api/bookings/hotels` POST and get 403 from Spring. Hiding the Staff link does not protect those APIs.

401 (except login failure) clears the token. Expired or Auth-restarted tokens look the same to the UI.

## 2-minute explanation

Flow: React → Gateway → Auth → JWT → React. Then React → Gateway → Booking/Food with Bearer. Food still Feigns to Booking; the browser never sees that.

Token in localStorage is a demo. XSS can steal it. httpOnly cookies need CSRF care. We chose the simple Axios header so the interview can show the `Authorization` line.

React is not in Compose. Recruiter demo: Compose up, `npm run dev`, login as customer, book, confirm, order food. Admin login shows the staff dashboard.

---

## Short interview answers

### Why doesn’t React call Booking directly?

**Short interview answer**

The frontend must not know internal hosts. One Gateway URL, one CORS policy, one JWT check at the edge.

### Is hiding `/admin` security?

**Short interview answer**

No. It is navigation. `@PreAuthorize` on the services is the authority.

### Where is the JWT stored and why is that a compromise?

**Short interview answer**

`localStorage` for a demo. Any XSS script can read it. Production often uses httpOnly cookies or a BFF.

### What happens on 401?

**Short interview answer**

Axios interceptor logs the user out, except for login/register (those 401s mean bad credentials).

### Why no Vite proxy to `/api`?

**Short interview answer**

A proxy would make the browser talk to `:5173` only and hide the Gateway. We want the demo to show `:8080`.
