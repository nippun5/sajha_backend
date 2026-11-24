-- Social identities never grant ADMIN roles. Email equality alone is not sufficient for linking.
ALTER TABLE app_users ADD COLUMN auth_version INT NOT NULL DEFAULT 0;

CREATE TABLE social_identities (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  provider VARCHAR(16) NOT NULL,
  provider_subject VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_social_provider_subject UNIQUE (provider,provider_subject),
  CONSTRAINT uq_social_user_provider UNIQUE (user_id,provider),
  CONSTRAINT fk_social_identity_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE social_signup_challenges (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  provider VARCHAR(16) NOT NULL,
  provider_subject VARCHAR(255) NOT NULL,
  email VARCHAR(254) NOT NULL,
  display_name VARCHAR(100),
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6),
  CONSTRAINT uq_social_signup_token UNIQUE (token_hash),
  INDEX idx_social_signup_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A one-time server-issued nonce binds each Apple identity token to a specific login attempt.
CREATE TABLE social_login_nonces (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  nonce_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6),
  CONSTRAINT uq_social_nonce_hash UNIQUE (nonce_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
