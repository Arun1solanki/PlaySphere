CREATE TABLE media_assets (
    id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(700) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NULL,
    content_type VARCHAR(100) NULL,
    size_bytes BIGINT NOT NULL,
    storage_provider VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_media_assets PRIMARY KEY (id),
    CONSTRAINT uk_media_assets_public_id UNIQUE (public_id),
    CONSTRAINT fk_media_assets_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teams (
    id VARCHAR(36) NOT NULL,
    captain_user_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sport VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    locality VARCHAR(80) NOT NULL,
    skill_level VARCHAR(32) NOT NULL,
    description VARCHAR(600) NULL,
    logo_url VARCHAR(700) NULL,
    max_members INT NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    join_mode VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT fk_teams_captain FOREIGN KEY (captain_user_id) REFERENCES users(id),
    CONSTRAINT uk_teams_name_city UNIQUE (name, city),
    INDEX idx_teams_discovery (status, sport, city, locality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_members (
    id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    member_role VARCHAR(24) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_team_members PRIMARY KEY (id),
    CONSTRAINT uk_team_member UNIQUE (team_id, user_id),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_team_members_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_join_requests (
    id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    applicant_user_id VARCHAR(36) NOT NULL,
    message VARCHAR(400) NULL,
    status VARCHAR(24) NOT NULL,
    decided_by_user_id VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    CONSTRAINT pk_team_join_requests PRIMARY KEY (id),
    CONSTRAINT fk_join_requests_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_applicant FOREIGN KEY (applicant_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_decider FOREIGN KEY (decided_by_user_id) REFERENCES users(id),
    INDEX idx_join_requests_team_status (team_id, status),
    INDEX idx_join_requests_applicant (applicant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recruitment_posts (
    id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    title VARCHAR(140) NOT NULL,
    sport VARCHAR(80) NOT NULL,
    positions_needed VARCHAR(300) NOT NULL,
    players_needed INT NOT NULL,
    skill_level VARCHAR(32) NOT NULL,
    city VARCHAR(80) NOT NULL,
    locality VARCHAR(80) NOT NULL,
    description VARCHAR(700) NULL,
    application_deadline DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_recruitment_posts PRIMARY KEY (id),
    CONSTRAINT fk_recruitment_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_recruitment_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    INDEX idx_recruitment_discovery (status, sport, city, locality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recruitment_applications (
    id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36) NOT NULL,
    applicant_user_id VARCHAR(36) NOT NULL,
    message VARCHAR(400) NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    CONSTRAINT pk_recruitment_applications PRIMARY KEY (id),
    CONSTRAINT uk_recruitment_application UNIQUE (post_id, applicant_user_id),
    CONSTRAINT fk_recruitment_application_post FOREIGN KEY (post_id) REFERENCES recruitment_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_recruitment_application_user FOREIGN KEY (applicant_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_recruitment_applications_post (post_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE turfs (
    id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(1000) NULL,
    address_line VARCHAR(220) NOT NULL,
    city VARCHAR(80) NOT NULL,
    locality VARCHAR(80) NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    sports VARCHAR(300) NOT NULL,
    amenities VARCHAR(700) NULL,
    base_price DECIMAL(12,2) NOT NULL,
    cover_image_url VARCHAR(700) NULL,
    status VARCHAR(24) NOT NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_turfs PRIMARY KEY (id),
    CONSTRAINT fk_turfs_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    INDEX idx_turfs_discovery (status, city, locality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE turf_slots (
    id VARCHAR(36) NOT NULL,
    turf_id VARCHAR(36) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_turf_slots PRIMARY KEY (id),
    CONSTRAINT fk_turf_slots_turf FOREIGN KEY (turf_id) REFERENCES turfs(id) ON DELETE CASCADE,
    CONSTRAINT uk_turf_slot UNIQUE (turf_id, start_at, end_at),
    INDEX idx_turf_slots_availability (turf_id, status, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id VARCHAR(36) NOT NULL,
    booking_code VARCHAR(32) NOT NULL,
    qr_token_hash VARCHAR(64) NOT NULL,
    player_user_id VARCHAR(36) NOT NULL,
    turf_id VARCHAR(36) NOT NULL,
    slot_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    cancellation_reason VARCHAR(400) NULL,
    checked_in_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uk_bookings_code UNIQUE (booking_code),
    INDEX idx_bookings_slot (slot_id),
    CONSTRAINT fk_bookings_player FOREIGN KEY (player_user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_turf FOREIGN KEY (turf_id) REFERENCES turfs(id),
    CONSTRAINT fk_bookings_slot FOREIGN KEY (slot_id) REFERENCES turf_slots(id),
    INDEX idx_bookings_player (player_user_id, created_at),
    INDEX idx_bookings_turf (turf_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
    id VARCHAR(36) NOT NULL,
    organizer_user_id VARCHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1200) NULL,
    sport VARCHAR(80) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    registration_type VARCHAR(24) NOT NULL,
    city VARCHAR(80) NOT NULL,
    locality VARCHAR(80) NOT NULL,
    venue_name VARCHAR(180) NULL,
    turf_id VARCHAR(36) NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    registration_deadline DATETIME(6) NOT NULL,
    min_players INT NOT NULL,
    max_players INT NOT NULL,
    entry_fee DECIMAL(12,2) NOT NULL,
    banner_url VARCHAR(700) NULL,
    rules VARCHAR(1500) NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_user_id) REFERENCES users(id),
    CONSTRAINT fk_events_turf FOREIGN KEY (turf_id) REFERENCES turfs(id),
    INDEX idx_events_discovery (status, sport, city, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_registrations (
    id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NULL,
    status VARCHAR(24) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    CONSTRAINT pk_event_registrations PRIMARY KEY (id),
    CONSTRAINT uk_event_registration UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_registration_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_event_registration_team FOREIGN KEY (team_id) REFERENCES teams(id),
    INDEX idx_event_registrations_event (event_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE matches (
    id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    home_name VARCHAR(140) NOT NULL,
    away_name VARCHAR(140) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    venue VARCHAR(180) NULL,
    home_score INT NULL,
    away_score INT NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_matches PRIMARY KEY (id),
    CONSTRAINT fk_matches_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_matches_event_schedule (event_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    reference_id VARCHAR(36) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_order_id VARCHAR(120) NULL,
    provider_payment_id VARCHAR(120) NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_payments_reference (purpose, reference_id),
    INDEX idx_payments_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refund_requests (
    id VARCHAR(36) NOT NULL,
    payment_id VARCHAR(36) NOT NULL,
    requested_by_user_id VARCHAR(36) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    decision_note VARCHAR(500) NULL,
    decided_by_user_id VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    CONSTRAINT pk_refund_requests PRIMARY KEY (id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_refund_requester FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_refund_decider FOREIGN KEY (decided_by_user_id) REFERENCES users(id),
    INDEX idx_refunds_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(600) NOT NULL,
    action_url VARCHAR(500) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_read (user_id, read_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversations (
    id VARCHAR(36) NOT NULL,
    conversation_type VARCHAR(24) NOT NULL,
    reference_id VARCHAR(36) NULL,
    title VARCHAR(160) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT fk_conversations_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversation_members (
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversation_members PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_conversation_members_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(36) NOT NULL,
    body VARCHAR(1200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users(id),
    INDEX idx_chat_messages_conversation (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id VARCHAR(36) NOT NULL,
    author_user_id VARCHAR(36) NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_review_author_target UNIQUE (author_user_id, target_type, target_id),
    CONSTRAINT fk_reviews_author FOREIGN KEY (author_user_id) REFERENCES users(id),
    INDEX idx_reviews_target (target_type, target_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reports (
    id VARCHAR(36) NOT NULL,
    reporter_user_id VARCHAR(36) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    reason VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    assigned_admin_user_id VARCHAR(36) NULL,
    resolution_note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users(id),
    CONSTRAINT fk_reports_admin FOREIGN KEY (assigned_admin_user_id) REFERENCES users(id),
    INDEX idx_reports_queue (status, priority, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id VARCHAR(36) NOT NULL,
    actor_user_id VARCHAR(36) NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id VARCHAR(80) NULL,
    details VARCHAR(1500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
