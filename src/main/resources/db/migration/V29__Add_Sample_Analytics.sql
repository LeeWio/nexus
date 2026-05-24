-- V29: Add sample analytics data for dashboard debugging

-- 1. Add some users
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `email`, `nickname`, `status`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
(2, 'user1', 'pass1', 'user1@example.com', 'John Doe', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 0),
(3, 'user2', 'pass2', 'user2@example.com', 'Jane Smith', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 0);

-- 2. Add some comments
INSERT IGNORE INTO `blog_comment` (`id`, `content`, `status`, `post_id`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
(1, 'Great post!', 'APPROVED', 1, NOW(), NOW(), 'GUEST', 0),
(2, 'Thanks for sharing.', 'PENDING', 1, NOW(), NOW(), 'GUEST', 0),
(3, 'Very helpful.', 'APPROVED', 2, NOW(), NOW(), 'GUEST', 0);

-- 3. Add some visit logs for today and yesterday
-- Today
INSERT INTO `sys_visit_log` (`ip_address`, `request_url`, `user_agent`, `visit_time`, `created_at`, `is_deleted`)
VALUES 
('127.0.0.1', '/post/welcome-to-nexus', 'Mozilla', NOW(), NOW(), 0),
('127.0.0.1', '/post/welcome-to-nexus', 'Mozilla', NOW(), NOW(), 0),
('192.168.1.1', '/post/java-21-features', 'Chrome', NOW(), NOW(), 0),
('10.0.0.1', '/', 'Safari', NOW(), NOW(), 0);

-- Yesterday
INSERT INTO `sys_visit_log` (`ip_address`, `request_url`, `user_agent`, `visit_time`, `created_at`, `is_deleted`)
VALUES 
('127.0.0.1', '/', 'Mozilla', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
('192.168.1.1', '/post/welcome-to-nexus', 'Chrome', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0);
