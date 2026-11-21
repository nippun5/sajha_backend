-- Phase 4D: safe, local-only media metadata. Bytes are in private filesystem (NOT MySQL).
CREATE TABLE marketplace_media (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  owner_id VARCHAR(36) NOT NULL,
  listing_id VARCHAR(36) NULL,
  storage_key VARCHAR(80) NOT NULL UNIQUE,
  mime_type VARCHAR(24) NOT NULL,
  byte_size BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_market_media_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
  CONSTRAINT fk_market_media_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id),
  INDEX idx_market_media_listing (listing_id, sort_order),
  INDEX idx_market_media_owner_staged (owner_id, listing_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
