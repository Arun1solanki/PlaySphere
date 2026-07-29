# Workflow reliability fixes and acceptance checklist

This revision addresses the role-by-role issues reported during local MVP testing. The existing sports-tech visual system and role colour themes are intentionally retained.

## Player

- Profile completion is persisted in `users.profile_completed`.
- Login and refresh repair older accounts whenever a matching `user_profiles` row already exists.
- Team creation/joining performs the same safe repair before rejecting the action.
- The Player turf page shows only real future `AVAILABLE` slots and a clear empty state when an owner has not published availability.
- Event registration reuses a previously cancelled registration row, allowing leave → join again.
- Uploaded local profile images render through the Vite `/uploads` proxy and in the role header.

Acceptance checks:

1. Complete a Player profile, sign out, and sign in again. The dashboard should open directly.
2. Create a Team after completing the profile.
3. Ask a Turf Owner to generate slots, then view and book one as Player.
4. Join an event, leave it, and join it again before the deadline.

## Turf Owner and Admin

- Turf form reset uses a saved form reference, preventing `Cannot read properties of null (reading 'reset')`.
- Turf location is selected by address search or browser location; latitude/longitude are stored automatically.
- Turf submission creates both an owner confirmation notification and Admin/Super Admin approval notifications.
- Admin approval sends the owner to Availability.
- Approved owners can generate regular slots for up to 31 days or add a custom slot.

Acceptance checks:

1. Search/select a map location and submit a Turf.
2. Confirm the owner notification and Admin pending request.
3. Approve the Turf as Admin.
4. Generate slots as the owner and verify they appear to Players and Organizers.

## Organizer

- Event creation requires an approved Turf and a real available slot.
- Publishing reserves the slot as `EVENT_RESERVED`; cancelling the event releases it.
- Organizers can generate fixtures from at least two active registrations or create a match manually.
- Public fixture views display actual fixture rows and scores.

Acceptance checks:

1. Select an approved Turf and slot while creating an Event.
2. Register at least two Players/Teams.
3. Generate fixtures and publish a score.
4. Cancel a future event and verify the Turf slot becomes available again.

## Database

Flyway migration `V6__workflow_reliability_and_event_venues.sql`:

- backfills incorrect profile-completion flags for existing profile rows;
- adds `events.turf_slot_id`;
- adds the foreign key and lookup index.

Do not delete the existing database. Restarting the updated backend applies V6 automatically.
