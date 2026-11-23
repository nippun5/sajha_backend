-- Phase 4F: real, user-owned business reviews and persisted moderation decisions.
CREATE TABLE business_reviews (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  business_id VARCHAR(36) NOT NULL,
  author_id VARCHAR(36) NOT NULL,
  stars INT NOT NULL,
  comment VARCHAR(600) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_review_business FOREIGN KEY(business_id) REFERENCES business_requests(id),
  CONSTRAINT fk_review_author FOREIGN KEY(author_id) REFERENCES app_users(id),
  CONSTRAINT uq_review_author_business UNIQUE(business_id,author_id),
  CONSTRAINT ck_review_stars CHECK (stars BETWEEN 1 AND 5),
  INDEX idx_review_business (business_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_review_reports (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  review_id VARCHAR(36) NOT NULL,
  reporter_id VARCHAR(36) NOT NULL,
  reason VARCHAR(80) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'open',
  moderator_id VARCHAR(36) NULL,
  moderator_note VARCHAR(800) NULL,
  created_at DATETIME(6) NOT NULL,
  moderated_at DATETIME(6) NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_review_report_review FOREIGN KEY(review_id) REFERENCES business_reviews(id),
  CONSTRAINT fk_review_report_reporter FOREIGN KEY(reporter_id) REFERENCES app_users(id),
  CONSTRAINT fk_review_report_moderator FOREIGN KEY(moderator_id) REFERENCES app_users(id),
  CONSTRAINT uq_review_reporter UNIQUE(review_id,reporter_id),
  INDEX idx_review_report_open (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
