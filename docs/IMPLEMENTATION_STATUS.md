# PlaySphere full-build status

This archive is a consolidated **full MVP**. The major product milestones are integrated into one Java 21/Spring Boot backend and one React/TypeScript frontend.

## Implemented

### Security and identity
- Registration as Player, Organizer, or Turf Owner
- Email verification, resend cooldown, and expiring password-reset links
- BCrypt password hashing
- JWT access tokens held in browser memory
- Session-specific refresh credentials in HttpOnly cookies with per-tab account isolation
- Server-side session/device records, idle expiry, revocation, and cross-tab logout
- Role-protected portals for Player, Organizer, Turf Owner, Admin, and Super Admin
- Profile onboarding, direct image upload only, Player discovery, and Indian mobile validation (`^\+91[6-9]\d{9}$`)
- Optional verified demo accounts for every role

### Teams and recruitment
- Team creation with automatic Team Captain membership
- Public discovery, open/request/invite-only join rules, enriched applicant profiles, capacity checks, and Captain decisions
- Need Players posts, applications, Captain approval/rejection, and automatic membership
- Team conversations and notifications
- Clan is intentionally removed; useful group features are consolidated into Teams

### Turfs and venue operations
- Turf Owner listings and Admin approval/rejection
- Address, location, sports, amenities, pricing, and media fields
- Availability slots with overlap prevention
- Slot blocking/reopening for maintenance
- Equipment/add-on inventory
- Pessimistic slot locking to prevent double booking
- Player booking, cancellation, history, one-time QR tokens, and Turf Owner check-in

### Events and matches
- Organizer event creation and cancellation
- Validated schedules, capacity, deadlines, individual/team registration rules
- Team events require the Team Captain and a matching active team
- Player registration/leaving and event conversation membership
- Organizer registration view, fixtures, scores, and results
- Event cancellation notifications

### Payments and refunds
- Trusted server-side amount calculation for bookings and event registrations
- Duplicate-payment prevention
- Development auto-payment adapter
- Razorpay order creation
- Browser Checkout integration
- Authenticated Checkout signature verification
- Razorpay webhook HMAC verification
- Payment history, Organizer/Turf Owner earnings, refund requests, and Admin decisions

### Communication, trust, and administration
- Persistent team/event conversations
- Authenticated STOMP subscriptions and REST history fallback
- In-app notifications
- Verified-participation reviews and duplicate-review prevention
- Reports, moderation queue, Admin resolution, and audit logs
- Admin views for users, teams, turfs, events, bookings, transactions, refunds, reviews, reports, and audits
- User suspension/blocking, turf decisions, review moderation, and refund decisions

### Storage and database
- MySQL + Flyway migrations `V1` through `V5`
- Local image adapter
- Cloudinary signed upload and deletion adapter
- Profile, Team, Turf, and Event forms use direct upload rather than arbitrary image URLs
- JPG/PNG/WebP validation and 5 MB upload limit

## Development adapters enabled by default

The project runs without external accounts using:

```env
APP_EMAIL_PROVIDER=log
APP_STORAGE_PROVIDER=local
APP_PAYMENT_PROVIDER=development
```

To replace them:

1. **Email log adapter → Brevo:** create an API key and verified sender/domain.
2. **Local image adapter → Cloudinary:** add cloud name, API key, and API secret.
3. **Development payment adapter → Razorpay:** add test credentials, frontend Key ID, and a webhook secret/URL.

The integration code is already present; account creation and credentials cannot be bundled in the source archive.

## Optional production integrations not selected yet

These are outside the development adapters and remain optional deployment/product decisions:

- Google Maps or Mapbox for map widgets, address autocomplete, and geocoding
- SMS provider for phone ownership OTP (format validation is implemented)
- Redis/RabbitMQ STOMP broker relay for multiple backend instances
- Private object storage/authenticated delivery for sensitive KYC or ownership documents
- Production hosting, DNS, TLS certificates, secrets manager, backups, monitoring, and CI/CD
- Advanced Organizer-to-Turf-Owner venue reservation/contract workflow beyond ordinary turf slots

## Verification limitation

The build environment performed structural, Java syntax, TypeScript offline type, JSON, YAML, XML, import, and migration-presence checks. External Maven/npm package downloads were unavailable in that environment, so the authoritative build must be run locally:

```powershell
cd backend
.\mvnw.cmd clean test
```

```powershell
cd frontend
npm install
npm run typecheck
npm run build
```
