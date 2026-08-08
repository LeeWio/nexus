-- V153: Bootstrap Admin User for just.vireo@gmail.com

-- 1. Ensure ROLE_ADMIN exists
INSERT IGNORE INTO `sys_role` (code, name, description, created_at, updated_at, created_by, last_modified_by, is_deleted, version) 
VALUES ('ROLE_ADMIN', 'Super Administrator', 'Has full access to all system functions', NOW(), NOW(), 'SYSTEM', 'SYSTEM', false, 0);

-- 2. Insert or update the user just.vireo@gmail.com
-- Password hash for 'Wei.Li.Laba00'
INSERT INTO `sys_user` (username, password, email, nickname, status, created_at, updated_at, created_by, last_modified_by, is_deleted, version, token_version)
VALUES ('just.vireo@gmail.com', '$2a$10$owpE74mjt1OCF3OEWAupeOtlKU9QFhFV7fyvGhxApHJgn00KJZjde', 'just.vireo@gmail.com', 'Admin', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 'SYSTEM', false, 0, 0)
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    email = VALUES(email),
    status = 'ACTIVE',
    is_deleted = false,
    updated_at = NOW();

-- 3. Assign ROLE_ADMIN to the user
INSERT IGNORE INTO `sys_user_role` (user_id, role_id)
SELECT u.id, r.id 
FROM `sys_user` u, `sys_role` r 
WHERE u.username = 'just.vireo@gmail.com' AND r.code = 'ROLE_ADMIN';
