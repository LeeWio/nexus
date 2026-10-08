-- Allow a comment to be authored by a guest nickname instead of a user account.
-- Each change is guarded so a partially applied attempt can be repaired.
SET @guest_name_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment' AND column_name = 'guest_name'
);
SET @sql := IF(@guest_name_exists = 0,
    'ALTER TABLE blog_comment MODIFY COLUMN user_id BIGINT NULL, ADD COLUMN guest_name VARCHAR(32) NULL AFTER user_id, ADD COLUMN guest_email VARCHAR(120) NULL AFTER guest_name, ADD COLUMN guest_token_hash CHAR(64) NULL AFTER guest_email',
    'ALTER TABLE blog_comment MODIFY COLUMN user_id BIGINT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @author_check_exists := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment'
      AND constraint_name = 'chk_comment_author' AND constraint_type = 'CHECK'
);
SET @sql := IF(@author_check_exists = 0,
    'ALTER TABLE blog_comment ADD CONSTRAINT chk_comment_author CHECK ((user_id IS NOT NULL AND guest_name IS NULL AND guest_token_hash IS NULL) OR (user_id IS NULL AND guest_name IS NOT NULL AND guest_token_hash IS NOT NULL))',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @guest_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment' AND index_name = 'idx_comment_guest_token'
);
SET @sql := IF(@guest_index_exists = 0,
    'CREATE INDEX idx_comment_guest_token ON blog_comment (guest_token_hash, client_request_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Idempotency records follow the same split: a user id or a guest token hash.
-- Nullable unique indexes treat every NULL as distinct, so each actor uses a
-- generated key that is NULL for the other kind of submission.
SET @guest_hash_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency' AND column_name = 'guest_token_hash'
);
SET @sql := IF(@guest_hash_exists = 0,
    'ALTER TABLE blog_comment_idempotency MODIFY COLUMN user_id BIGINT NULL, ADD COLUMN guest_token_hash CHAR(64) NULL AFTER user_id',
    'ALTER TABLE blog_comment_idempotency MODIFY COLUMN user_id BIGINT NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_key_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency' AND column_name = 'user_idempotency_key'
);
SET @sql := IF(@user_key_exists = 0,
    'ALTER TABLE blog_comment_idempotency ADD COLUMN user_idempotency_key VARCHAR(80) GENERATED ALWAYS AS (IF(user_id IS NULL, NULL, idempotency_key)) STORED, ADD COLUMN guest_idempotency_key VARCHAR(80) GENERATED ALWAYS AS (IF(guest_token_hash IS NULL, NULL, idempotency_key)) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- The user foreign key currently rides on the old unique index. Create the
-- replacement first, then move the foreign key before dropping that index.
SET @actor_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency'
      AND index_name = 'uk_comment_idempotency_user_actor'
);
SET @sql := IF(@actor_index_exists = 0,
    'CREATE UNIQUE INDEX uk_comment_idempotency_user_actor ON blog_comment_idempotency (user_id, user_idempotency_key)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @guest_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency'
      AND index_name = 'uk_comment_idempotency_guest_key'
);
SET @sql := IF(@guest_index_exists = 0,
    'CREATE UNIQUE INDEX uk_comment_idempotency_guest_key ON blog_comment_idempotency (guest_token_hash, guest_idempotency_key)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency'
      AND index_name = 'uk_comment_idempotency_user_key'
);
SET @sql := IF(@old_index_exists = 0, 'SELECT 1',
    'ALTER TABLE blog_comment_idempotency DROP FOREIGN KEY fk_comment_idempotency_user');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := IF(@old_index_exists = 0, 'SELECT 1',
    'ALTER TABLE blog_comment_idempotency DROP INDEX uk_comment_idempotency_user_key');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := IF(@old_index_exists = 0, 'SELECT 1',
    'ALTER TABLE blog_comment_idempotency ADD CONSTRAINT fk_comment_idempotency_user FOREIGN KEY (user_id) REFERENCES sys_user(id)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @actor_check_exists := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment_idempotency'
      AND constraint_name = 'chk_comment_idempotency_actor'
);
SET @sql := IF(@actor_check_exists = 0,
    'ALTER TABLE blog_comment_idempotency ADD CONSTRAINT chk_comment_idempotency_actor CHECK ((user_id IS NOT NULL AND guest_token_hash IS NULL) OR (user_id IS NULL AND guest_token_hash IS NOT NULL))',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
