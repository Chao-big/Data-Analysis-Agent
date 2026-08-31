-- Auth login audit refinement
-- Date: 2026-08-28
-- Scope:
-- 1. Keep only successful login/register audit records
-- 2. Replace client_ip with client_public_ip
-- 3. Drop failure-related columns and indexes

DELETE FROM auth_login_log
WHERE user_id IS NULL
   OR login_result IS NULL
   OR UPPER(login_result) <> 'SUCCESS';

UPDATE auth_login_log login_log
JOIN auth_user auth_user_record ON auth_user_record.id = login_log.user_id
SET login_log.username = auth_user_record.username;

ALTER TABLE auth_login_log
    DROP INDEX idx_auth_login_log_result,
    DROP INDEX idx_auth_login_log_user_result_time;

ALTER TABLE auth_login_log
    DROP COLUMN login_result,
    DROP COLUMN failure_reason,
    RENAME COLUMN client_ip TO client_public_ip;

ALTER TABLE auth_login_log
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT 'Authenticated user ID',
    MODIFY COLUMN username VARCHAR(64) NOT NULL COMMENT 'Authenticated username',
    MODIFY COLUMN client_public_ip VARCHAR(64) NULL COMMENT 'Browser public IP or trusted proxy source IP',
    MODIFY COLUMN user_agent VARCHAR(512) NULL COMMENT 'Client user agent',
    MODIFY COLUMN login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Successful login time',
    ADD KEY idx_auth_login_log_user_time (user_id, login_at),
    ADD KEY idx_auth_login_log_public_ip (client_public_ip);
