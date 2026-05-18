-- V23: Create specific admin user and assign highest permissions

-- 1. Ensure ROLE_ADMIN exists
INSERT INTO `sys_role` (code, name, description, created_at, updated_at)
SELECT 'ROLE_ADMIN', 'Administrator', 'Highest level of access', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE code = 'ROLE_ADMIN');

-- 2. Create the user 'liwei' if not exists
INSERT INTO `sys_user` (username, password, email, nickname, status, created_at, updated_at)
SELECT 'liwei', '$2a$10$0Lgr63yXQREaJe4A3xMqmuEtBmTNiOpDXBi4FRuLVZBUtNLnTgTe2', '3499508634@qq.com', 'Wei Li', 'ACTIVE', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE username = 'liwei' OR email = '3499508634@qq.com');

-- 3. Assign ROLE_ADMIN to the user
-- Using a subquery to find IDs and ignoring if the relation already exists
INSERT INTO `sys_user_role` (user_id, role_id)
SELECT u.id, r.id
FROM `sys_user` u
JOIN `sys_role` r ON r.code = 'ROLE_ADMIN'
WHERE u.username = 'liwei'
AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);
