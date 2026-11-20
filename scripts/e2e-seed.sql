SET @now = UTC_TIMESTAMP(3);

INSERT INTO sys_user (
    created_by,
    username,
    password,
    token_version,
    email,
    nickname,
    status,
    created_at,
    updated_at,
    last_modified_by,
    version,
    is_deleted
)
VALUES (
    'e2e',
    'e2e-reader',
    '$2a$10$Uu2ATuDqomlijBX4sr86.ucG.DUTrtZju/nrNfAnO5Yzgl0LaiiAi',
    0,
    'e2e-reader@nexus.test',
    'E2E Reader',
    'ACTIVE',
    @now,
    @now,
    'e2e',
    0,
    false
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    status = 'ACTIVE',
    is_deleted = false,
    updated_at = VALUES(updated_at);

SET @reader_id = (SELECT id FROM sys_user WHERE username = 'e2e-reader');
SET @user_role_id = (SELECT id FROM sys_role WHERE code = 'ROLE_USER');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (@reader_id, @user_role_id);

INSERT INTO blog_category (created_by, name, slug, description, created_at, updated_at, last_modified_by, version, is_deleted)
VALUES ('e2e', 'E2E Engineering', 'e2e-engineering', 'Disposable notification journey category', @now, @now, 'e2e', 0, false)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_deleted = false,
    updated_at = VALUES(updated_at);

SET @category_id = (SELECT id FROM blog_category WHERE slug = 'e2e-engineering');

INSERT INTO blog_category_follow (user_id, category_id, created_by, created_at, updated_at, last_modified_by, is_deleted)
VALUES (@reader_id, @category_id, 'e2e', @now, @now, 'e2e', false)
ON DUPLICATE KEY UPDATE
    is_deleted = false,
    updated_at = VALUES(updated_at);
