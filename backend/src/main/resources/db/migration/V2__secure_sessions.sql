ALTER TABLE refresh_tokens
    ADD COLUMN last_used_at DATETIME(6) NULL AFTER created_at,
    ADD COLUMN user_agent VARCHAR(255) NULL AFTER last_used_at,
    ADD COLUMN ip_address VARCHAR(64) NULL AFTER user_agent,
    ADD COLUMN persistent_login BOOLEAN NOT NULL DEFAULT FALSE AFTER ip_address;

UPDATE refresh_tokens
SET last_used_at = created_at
WHERE last_used_at IS NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN last_used_at DATETIME(6) NOT NULL;

CREATE INDEX idx_refresh_token_active_session
    ON refresh_tokens (user_id, revoked_at, last_used_at);
