-- Auth module MVP schema
-- Date: 2026-08-27
-- Scope:
-- 1. auth_user
-- 2. auth_login_log

CREATE TABLE IF NOT EXISTS auth_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    username VARCHAR(64) NOT NULL COMMENT 'Global unique login username',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt password hash',
    nickname VARCHAR(64) NOT NULL COMMENT 'User nickname',
    avatar_url VARCHAR(512) NULL COMMENT 'User avatar URL',
    email VARCHAR(128) NOT NULL COMMENT 'Global unique email',
    phone VARCHAR(32) NOT NULL COMMENT 'Global unique phone',
    gender VARCHAR(16) NULL COMMENT 'Optional gender field',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, DISABLED, LOCKED',
    last_login_at DATETIME NULL COMMENT 'Last successful login time',
    last_login_ip VARCHAR(64) NULL COMMENT 'Last successful login IP',
    remark VARCHAR(255) NULL COMMENT 'Internal remark',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_user_username (username),
    UNIQUE KEY uk_auth_user_email (email),
    UNIQUE KEY uk_auth_user_phone (phone),
    KEY idx_auth_user_status (status),
    KEY idx_auth_user_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Auth users';

CREATE TABLE IF NOT EXISTS auth_login_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT NULL COMMENT 'User ID when matched',
    username VARCHAR(64) NOT NULL COMMENT 'Submitted username',
    login_result VARCHAR(32) NOT NULL COMMENT 'SUCCESS or FAILED',
    failure_reason VARCHAR(255) NULL COMMENT 'Failure reason when login failed',
    client_ip VARCHAR(64) NULL COMMENT 'Client IP',
    user_agent VARCHAR(512) NULL COMMENT 'Client user agent',
    login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Login attempt time',
    PRIMARY KEY (id),
    KEY idx_auth_login_log_user_id (user_id),
    KEY idx_auth_login_log_username (username),
    KEY idx_auth_login_log_result (login_result),
    KEY idx_auth_login_log_login_at (login_at),
    KEY idx_auth_login_log_user_result_time (user_id, login_result, login_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Auth login audit log';
