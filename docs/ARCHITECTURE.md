# Architecture

```text
Browser
  └─ React + TypeScript + Vite
       ├─ Public discovery
       ├─ Player portal (cyan)
       ├─ Organizer portal (violet)
       ├─ Turf Owner portal (green)
       ├─ Admin portal (amber)
       └─ Super Admin portal (red)
            │ REST + JWT / STOMP WebSocket
            ▼
Java 21 + Spring Boot modular monolith
  ├─ auth + secure server-side sessions
  ├─ user + profile + role authorization
  ├─ team + recruitment
  ├─ turf + slots + equipment + bookings + check-in
  ├─ event + registrations + matches + results
  ├─ payment + refunds + Razorpay verification
  ├─ chat + notifications
  ├─ reviews + reports
  ├─ admin + audit
  └─ media + email provider interfaces
            │
            ├─ MySQL + Flyway
            ├─ local media / Cloudinary
            ├─ log email / Brevo
            └─ development payment / Razorpay
```

## Design decisions

- One deployable backend and one frontend for the MVP.
- Modules are separated by package, service, repository, and authorization boundaries.
- MySQL is the permanent source of truth.
- Flyway owns production schema changes; Hibernate validates the schema.
- External services are hidden behind provider interfaces so local development does not require paid accounts.
- Refresh credentials remain in HttpOnly cookies; access tokens stay in browser memory.
- Backend authorization is authoritative; hidden frontend controls are only a usability layer.
- The STOMP simple broker targets a single backend instance. A broker relay is the scale-out path.
