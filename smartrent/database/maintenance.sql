-- Run against the existing SmartRent database before starting
-- a backend build containing the maintenance feature.
-- This script is applied manually; it is not an automatic migration.

CREATE TABLE IF NOT EXISTS maintenance_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rental_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_maintenance_rental (rental_id),
    CONSTRAINT fk_maintenance_rental
        FOREIGN KEY (rental_id) REFERENCES rentals(id),
    CONSTRAINT chk_maintenance_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED'))
);
