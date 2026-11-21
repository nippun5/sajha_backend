-- Phase 4C: additive migration. NEVER edit V1 on a database that has run it.
CREATE TABLE marketplace_listings (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  owner_id VARCHAR(36) NOT NULL,
  owner_display_name VARCHAR(100) NOT NULL,
  kind VARCHAR(16) NOT NULL,
  title VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  contact_phone VARCHAR(16) NOT NULL,
  location VARCHAR(180) NOT NULL,
  details_json LONGTEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  idempotency_key VARCHAR(80) NULL,
  request_fingerprint VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_listing_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
  CONSTRAINT uq_listing_idempotent UNIQUE (owner_id, idempotency_key),
  INDEX idx_listing_public (status, kind, created_at),
  INDEX idx_listing_owner (owner_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE saved_marketplace_listings (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  listing_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_saved_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_saved_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE CASCADE,
  CONSTRAINT uq_saved_listing UNIQUE (user_id, listing_id),
  INDEX idx_saved_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
