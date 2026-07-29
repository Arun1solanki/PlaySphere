# Useful logic reviewed from the previous .NET project

The uploaded .NET project was treated as a reference, not as a source of truth. The current Java 21/Spring Boot MVP remains the base and its existing visual design is unchanged.

## Adopted and improved

### Per-tab login isolation

The old frontend stored its JWT in `sessionStorage`, which naturally isolated ordinary tabs but exposed the token to JavaScript. The Java project now keeps only a **non-secret session UUID** in `sessionStorage`; the actual refresh credential remains in a session-specific **HttpOnly cookie**. This preserves per-tab account isolation without copying the older security weakness.

### Teams

Useful team behavior was carried over and expanded:

- Team creation makes the creator Team Captain.
- Public/private discovery and sport/text filtering.
- Captain and member profile summaries.
- Join requests with applicant name, photo, skill, position, and location.
- Accept/reject decisions, capacity checks, duplicate-request prevention, member removal, leaving, captaincy transfer, and archival.
- Team conversation membership is updated automatically when members join or leave.

### Need Players

- Captains publish recruitment posts for their own active teams.
- Applications contain applicant profile summaries rather than raw user IDs.
- Captains accept or reject applications.
- Accepted applicants become members and enter the team conversation automatically.
- Capacity and duplicate-application checks remain server-side.

### Chat

- Team and event conversations are created automatically.
- Membership is checked before reading or sending messages.
- Message responses include sender display name and profile image.
- REST history remains available alongside STOMP live delivery.

### Email verification

The useful verification flow was retained, but the Java implementation remains stronger: random expiring tokens are stored only as SHA-256 hashes, links are single-use, resend cooldowns are enforced, and Brevo/log providers share one backend interface.

## Intentionally not copied

- **Clans:** removed by product decision; useful group behavior is consolidated into Teams.
- **JWT stored directly in sessionStorage:** replaced by in-memory access tokens and HttpOnly refresh credentials.
- **Arbitrary image URL fields:** profile, team, turf, and event media must be uploaded through PlaySphere.
- **Local-only image assumptions:** the Java MVP supports both local development storage and signed Cloudinary uploads.
- Any incomplete, duplicate, or role-bypassing behavior from the old project.
