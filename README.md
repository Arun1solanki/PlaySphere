# PlaySphere — Consolidated Full MVP

PlaySphere is a role-aware sports platform built with **React + TypeScript + Vite** and **Java 21 + Spring Boot**.

## Product modules

- Secure registration, email verification, password reset, login, per-tab refresh-cookie sessions, device revocation, and account-specific cross-tab logout
- Distinct Player, Organizer, Turf Owner, Admin, and Super Admin portals
- Validated profiles, direct image upload, and Player discovery
- Teams, Captains, members, join requests, and Need Players recruitment
- Turfs, approval, slots, maintenance blocking, equipment, bookings, payments, QR/check-in
- Events, individual/team registration, fixtures, scores, and cancellation
- Persistent chat, STOMP live updates, notifications, reviews, reports, moderation, and audit logs
- Development/Brevo email, local/Cloudinary media, and development/Razorpay payment providers
- No Clan module; relevant group capabilities are merged into Teams

## Prerequisites

- Java 21
- MySQL 8.x
- Node.js 20.19+ or 22.12+
- Internet access for the first Maven/npm dependency download

## Setup

From the project root:

```powershell
Copy-Item .env.example .env
```

Set at minimum:

```env
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
APP_SEED_DEMO_ACCOUNTS=true
APP_DEMO_PASSWORD=Demo@12345
```

The JDBC URL can create `playsphere` when the MySQL user has permission. Flyway applies migrations `V1`–`V5`.

## Run

Backend:

```powershell
.\run-backend.cmd
```

Frontend in another terminal:

```powershell
.\run-frontend.cmd
```

- Application: `http://localhost:5173`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Demo accounts

Enable `APP_SEED_DEMO_ACCOUNTS=true` locally.

| Role | Email | Password |
|---|---|---|
| Player | `player@playsphere.local` | `Demo@12345` |
| Organizer | `organizer@playsphere.local` | `Demo@12345` |
| Turf Owner | `owner@playsphere.local` | `Demo@12345` |
| Admin | `admin@playsphere.local` | `Demo@12345` |
| Super Admin | `superadmin@playsphere.local` | `Demo@12345` |

Disable demo seeding outside local development.

## Important revision notes

- The existing sports-tech visual design and role colour themes are unchanged.
- Browser tabs now use independent, session-specific HttpOnly refresh cookies selected by a non-secret tab session ID. Player, Organizer, and Admin accounts can therefore remain logged in independently in separate tabs.
- Profile, Team, Turf, and Event media are uploaded directly; arbitrary image-URL fields are not shown.
- Useful Team, recruitment, chat, and email-verification behavior from the supplied .NET project was reviewed and reimplemented within the Java/Spring architecture. Clans were not restored.

### Multi-role tab test

1. In Tab A, log in as `player@playsphere.local`.
2. Open a fresh Tab B, visit `/login`, and log in as `admin@playsphere.local`.
3. Open a fresh Tab C and log in as `organizer@playsphere.local`.
4. Refresh all three tabs. Each tab should retain its own account and role.

A browser's **Duplicate tab** command may copy `sessionStorage` initially. Logging in with another account in that duplicated tab creates a new independent session without changing the original tab.

## Provider modes

The default local configuration needs no third-party credentials:

```env
APP_EMAIL_PROVIDER=log
APP_STORAGE_PROVIDER=local
APP_PAYMENT_PROVIDER=development
```

See:

- `docs/IMPLEMENTATION_STATUS.md`
- `docs/THIRD_PARTY_SETUP.md`
- `docs/DEVELOPMENT_ADAPTERS.md`
- `docs/SECURE_SESSIONS.md`
- `docs/DOTNET_LOGIC_REVIEW.md`
- `docs/BUILD_VERIFICATION.md`

## Verify locally

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm install
npm run typecheck
npm run build
npm run dev
```
