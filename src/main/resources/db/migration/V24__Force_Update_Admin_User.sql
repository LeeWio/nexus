-- V24: Force update the password and role for the requested admin account

-- 1. Ensure the user exists and has the correct password/status
-- If they don't exist, this does nothing (V23 should have created them, but we cover all bases)
UPDATE `sys_user` 
SET `password` = '$2a$10$0Lgr63yXQREaJe4A3xMqmuEtBmTNiOpDXBi4FRuLVZBUtNLnTgTe2',
    `status` = 'ACTIVE',
    `updated_at` = NOW()
WHERE `username` = 'liwei' OR `email` = '3499508634@qq.com';

-- 2. Ensure ROLE_ADMIN exists (idempotent)
INSERT INTO `sys_role` (code, name, description, created_at, updated_at)
SELECT 'ROLE_ADMIN', 'Administrator', 'Highest level of access', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE code = 'ROLE_ADMIN');

-- 3. Force link the user to ROLE_ADMIN
INSERT INTO `sys_user_role` (user_id, role_id)
SELECT u.id, r.id
FROM `sys_user` u, `sys_role` r
WHERE (u.username = 'liwei' OR u.email = '3499508634@qq.com')
AND r.code = 'ROLE_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);
