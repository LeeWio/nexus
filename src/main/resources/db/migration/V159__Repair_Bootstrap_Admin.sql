-- Repair the V153 bootstrap path without modifying an already-applied migration.
-- V153 could match an existing row by email, replace its password, and then miss
-- the same row while assigning ROLE_ADMIN because that lookup used username only.

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u
JOIN `sys_role` r ON r.code = 'ROLE_ADMIN' AND r.is_deleted = false
WHERE u.email = 'just.vireo@gmail.com'
  AND u.is_deleted = false;

-- The credential embedded by V153 is present in repository history and must no
-- longer authenticate. Only invalidate the exact legacy hash so a password that
-- has already been rotated is never overwritten. The user can recover through
-- the normal password-reset flow once the application is healthy.
UPDATE `sys_user`
SET password = CONCAT('RESET_REQUIRED_', SHA2(CONCAT(UUID(), RAND()), 256)),
    token_version = token_version + 1,
    updated_at = NOW()
WHERE email = 'just.vireo@gmail.com'
  AND password = '$2a$10$owpE74mjt1OCF3OEWAupeOtlKU9QFhFV7fyvGhxApHJgn00KJZjde';
