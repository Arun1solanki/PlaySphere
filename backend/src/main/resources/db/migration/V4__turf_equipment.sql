CREATE TABLE turf_equipment (
    id VARCHAR(36) NOT NULL,
    turf_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    quantity INT NOT NULL,
    price_per_booking DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_turf_equipment PRIMARY KEY (id),
    CONSTRAINT fk_turf_equipment_turf FOREIGN KEY (turf_id) REFERENCES turfs(id) ON DELETE CASCADE,
    CONSTRAINT uk_turf_equipment_name UNIQUE (turf_id, name),
    INDEX idx_turf_equipment_active (turf_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
