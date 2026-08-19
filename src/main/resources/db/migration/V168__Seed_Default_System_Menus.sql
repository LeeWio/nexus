-- V168: Seed Default System Menus for Odyssey Dashboard

DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

-- 1. Insert Top-Level Directories
INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort_order, is_visible, is_public, created_at, updated_at) VALUES
(1, 0, 'Overview', '', 'overview:view', 0, 10, true, false, NOW(), NOW()),
(2, 0, 'Content', '', 'content:view', 0, 20, true, false, NOW(), NOW()),
(3, 0, 'Resources', '', 'resources:view', 0, 30, true, false, NOW(), NOW()),
(4, 0, 'Users & Access', '', 'access:view', 0, 40, true, false, NOW(), NOW()),
(5, 0, 'System', '', 'system:view', 0, 50, true, false, NOW(), NOW());

-- 2. Insert Menus
INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort_order, is_visible, is_public, created_at, updated_at) VALUES
(10, 1, 'Dashboard', '/', 'dashboard:view', 1, 110, true, false, NOW(), NOW()),
(11, 1, 'Tracker', '/tracker', 'tracker:view', 1, 120, true, false, NOW(), NOW()),
(12, 1, 'Analytics', '/analytics', 'analytics:view', 1, 130, true, false, NOW(), NOW()),
(20, 2, 'Posts', '/posts', 'posts:view', 1, 210, true, false, NOW(), NOW()),
(21, 2, 'Columns', '/columns', 'columns:view', 1, 220, true, false, NOW(), NOW()),
(22, 2, 'Categories', '/categories', 'categories:view', 1, 230, true, false, NOW(), NOW()),
(23, 2, 'Tags', '/tags', 'tags:view', 1, 240, true, false, NOW(), NOW()),
(24, 2, 'Comments', '/comments', 'comments:view', 1, 250, true, false, NOW(), NOW()),
(25, 2, 'Moments', '/moments', 'moments:view', 1, 260, true, false, NOW(), NOW()),
(30, 3, 'Materials', '/files', 'files:view', 1, 310, true, false, NOW(), NOW()),
(31, 3, 'Friend Links', '/links', 'links:view', 1, 320, true, false, NOW(), NOW()),
(32, 3, 'Orders', '/orders', 'orders:view', 1, 330, true, false, NOW(), NOW()),
(40, 4, 'Users', '/users', 'users:view', 1, 410, true, false, NOW(), NOW()),
(41, 4, 'Groups', '/groups', 'groups:view', 1, 420, true, false, NOW(), NOW()),
(42, 4, 'Roles', '/roles', 'roles:view', 1, 430, true, false, NOW(), NOW()),
(43, 4, 'Permissions', '/permissions', 'permissions:view', 1, 440, true, false, NOW(), NOW()),
(50, 5, 'Settings', '/settings', 'settings:view', 1, 510, true, false, NOW(), NOW());

-- 3. Insert Actions
INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort_order, is_visible, is_public, created_at, updated_at) VALUES
(201, 20, 'Create Post', '', 'post:create', 2, 2010, false, false, NOW(), NOW()),
(202, 20, 'Edit Post', '', 'post:edit', 2, 2020, false, false, NOW(), NOW()),
(203, 20, 'Delete Post', '', 'post:delete', 2, 2030, false, false, NOW(), NOW()),
(241, 24, 'Approve Comment', '', 'comment:approve', 2, 2410, false, false, NOW(), NOW()),
(242, 24, 'Reject Comment', '', 'comment:reject', 2, 2420, false, false, NOW(), NOW()),
(243, 24, 'Delete Comment', '', 'comment:delete', 2, 2430, false, false, NOW(), NOW()),
(251, 25, 'Create Moment', '', 'moment:create', 2, 2510, false, false, NOW(), NOW()),
(252, 25, 'Delete Moment', '', 'moment:delete', 2, 2520, false, false, NOW(), NOW()),
(401, 40, 'Deactivate User', '', 'user:status', 2, 4010, false, false, NOW(), NOW()),
(402, 40, 'Modify User Roles', '', 'user:roles', 2, 4020, false, false, NOW(), NOW());

-- 4. Associate to ROLE_ADMIN
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM sys_role r, sys_menu m 
WHERE r.code = 'ROLE_ADMIN';
