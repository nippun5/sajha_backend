CREATE TABLE password_reset_tokens (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  CONSTRAINT uq_password_reset_hash UNIQUE (token_hash),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
  INDEX idx_password_reset_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
