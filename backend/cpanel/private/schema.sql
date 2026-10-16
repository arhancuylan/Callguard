-- Import ONLY into the new, empty CallGuard database through phpMyAdmin.
-- InnoDB transactions and utf8mb4 are required. No DROP, CREATE DATABASE or MK Tech tables.
CREATE TABLE cg_users (
 id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(255) NULL,
 verified TINYINT(1) NOT NULL DEFAULT 0, pro TINYINT(1) NOT NULL DEFAULT 0,
 created_ms BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_trials (
 user_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, started_ms BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_sessions (
 token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 user_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, expires_ms BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES cg_users(id) ON DELETE CASCADE, INDEX (expires_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_email_tokens (
 token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 user_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 kind VARCHAR(16) CHARACTER SET ascii NOT NULL, expires_ms BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES cg_users(id) ON DELETE CASCADE, INDEX (expires_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_backups (
 user_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 revision INT NOT NULL, data MEDIUMTEXT CHARACTER SET ascii NOT NULL, updated_ms BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES cg_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_purchases (
 token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 user_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 token_encrypted TEXT CHARACTER SET ascii NOT NULL, updated_ms BIGINT NOT NULL, INDEX(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_auth_challenges (
 nonce_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, expires_ms BIGINT NOT NULL, INDEX(expires_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE cg_rate_limits (
 bucket_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 window_id BIGINT NOT NULL, hits INT NOT NULL, INDEX(window_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
