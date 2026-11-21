CREATE TABLE app_users (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(16) NOT NULL,
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_app_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
  user_id VARCHAR(36) NOT NULL,
  role VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id,role),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE verified_contact_phones (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  phone VARCHAR(16) NOT NULL,
  verified_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_verified_contact_phones_number UNIQUE(phone),
  CONSTRAINT fk_verified_contact_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  INDEX idx_verified_contact_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_sessions (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  family_id VARCHAR(36) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  replaced_by VARCHAR(36) NULL,
  CONSTRAINT uq_refresh_token_hash UNIQUE(token_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  INDEX idx_refresh_family (family_id),
  INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE phone_challenges (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  phone VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  completed_at DATETIME(6) NULL,
  CONSTRAINT fk_challenge_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  INDEX idx_challenge_user_created (user_id,created_at),
  INDEX idx_challenge_phone_created (phone,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
