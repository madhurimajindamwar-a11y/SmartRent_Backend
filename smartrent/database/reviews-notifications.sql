CREATE TABLE IF NOT EXISTS property_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    property_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_review_property_tenant (property_id, tenant_id),
    CONSTRAINT fk_review_property
        FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_review_tenant
        FOREIGN KEY (tenant_id) REFERENCES users(id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    target_path VARCHAR(200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_notification_recipient (recipient_id, id),
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id)
);
