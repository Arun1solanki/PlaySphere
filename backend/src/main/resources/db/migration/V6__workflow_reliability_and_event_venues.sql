-- Repair profile completion state for accounts that already have a saved profile.
UPDATE users u
SET u.profile_completed = TRUE
WHERE u.profile_completed = FALSE
  AND EXISTS (
      SELECT 1
      FROM user_profiles p
      WHERE p.user_id = u.id
  );

-- Reserve a concrete turf slot for each hosted event.
ALTER TABLE events
    ADD COLUMN turf_slot_id VARCHAR(36) NULL AFTER turf_id;

ALTER TABLE events
    ADD CONSTRAINT fk_events_turf_slot
        FOREIGN KEY (turf_slot_id) REFERENCES turf_slots(id);

CREATE INDEX idx_events_turf_slot ON events(turf_slot_id);
