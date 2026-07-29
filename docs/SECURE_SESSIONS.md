# Secure per-tab sessions

PlaySphere uses short-lived JWT access tokens plus revocable server-side browser sessions.

## Storage design

- **Access token:** React memory only; never `localStorage` or `sessionStorage`.
- **Tab session identifier:** a non-secret UUID in `sessionStorage`.
- **Refresh credential:** a random opaque value in a session-specific `HttpOnly` cookie.
- **Database:** only the SHA-256 hash of each refresh credential is stored.

The tab sends `X-PlaySphere-Session: <session UUID>`. The backend uses that non-secret UUID to select the matching HttpOnly refresh cookie. JavaScript cannot read the refresh credential.

## Why sessions no longer merge

Each successful login creates a new database session and a separate cookie name derived from its session UUID. Therefore:

- Tab A can stay logged in as Player.
- Tab B can log in as Admin.
- Tab C can log in as Organizer.
- Refreshing any tab restores only that tab's account and role.

Logging in from one tab does not revoke the previous session automatically, because a duplicated browser tab may still be using it. Old sessions can be revoked from **Manage Sessions** or with **Log out from every device**.

A fresh tab created by typing or pasting a PlaySphere URL has no tab session UUID and is redirected to login. A browser's **Duplicate tab** command may initially copy `sessionStorage`; logging in with another account in the duplicate creates a new session without changing the original tab.

## Controls

- Each JWT contains a `sid` claim and is accepted only while that database session remains active.
- Account suspension, logout, session revocation, absolute expiry, and inactivity expiry invalidate protected requests.
- Role-aware inactivity defaults: Player 60 minutes, Organizer 45, Turf Owner 45, Admin 20, Super Admin 15.
- **Log out** affects the current tab/session only.
- **Log out from every device** revokes every session for that user and synchronizes only tabs logged in as that same user.
- Refresh requests require both the per-tab session header and the matching SameSite HttpOnly cookie.
- Enable `SESSION_COOKIE_SECURE=true` when running under HTTPS.

## Endpoints

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `GET /api/auth/me`
- `GET /api/auth/sessions`
- `DELETE /api/auth/sessions/{sessionId}`
