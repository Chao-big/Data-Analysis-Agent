ALTER TABLE auth_user
    ADD COLUMN gender_tmp SMALLINT NOT NULL DEFAULT 0 COMMENT '0 unknown, 1 male, 2 female' AFTER phone;

UPDATE auth_user
SET gender_tmp = CASE
    WHEN gender IS NULL OR TRIM(gender) = '' THEN 0
    WHEN LOWER(TRIM(gender)) IN ('0', 'unknown', 'unset', 'secret', 'private') THEN 0
    WHEN TRIM(gender) IN ('保密', '未知') THEN 0
    WHEN LOWER(TRIM(gender)) IN ('1', 'male', 'man') THEN 1
    WHEN TRIM(gender) = '男' THEN 1
    WHEN LOWER(TRIM(gender)) IN ('2', 'female', 'woman') THEN 2
    WHEN TRIM(gender) = '女' THEN 2
    ELSE 0
END;

ALTER TABLE auth_user
    DROP COLUMN gender,
    CHANGE COLUMN gender_tmp gender SMALLINT NOT NULL DEFAULT 0 COMMENT '0 unknown, 1 male, 2 female';
