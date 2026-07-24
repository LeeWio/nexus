-- V30: Add 14 days of historical analytics data for rich chart visualization

-- Clean up previous debug logs to ensure a clean trend
DELETE FROM `sys_visit_log` WHERE ip_address IN ('127.0.0.1', '192.168.1.1', '10.0.0.1');

-- Helper for generating random-ish data over 14 days
-- We'll insert batches for each day with varying counts

-- 14 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) VALUES 
('1.1.1.1', '/', 'Mozilla', NOW() - INTERVAL '14' DAY, NOW() - INTERVAL '14' DAY, 0),
('1.1.1.2', '/post/java-21-features', 'Mozilla', NOW() - INTERVAL '14' DAY, NOW() - INTERVAL '14' DAY, 0),
('1.1.1.3', '/post/welcome-to-nexus', 'Mozilla', NOW() - INTERVAL '14' DAY, NOW() - INTERVAL '14' DAY, 0);

-- 10 days ago (Spike)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('2.2.2.', id), '/post/modern-frontend-react', 'Chrome', NOW() - INTERVAL '10' DAY, NOW() - INTERVAL '10' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) as t;

-- 7 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('3.3.3.', id), '/', 'Safari', NOW() - INTERVAL '7' DAY, NOW() - INTERVAL '7' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) as t;

-- 5 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('4.4.4.', id), '/post/building-search-elasticsearch', 'Mozilla', NOW() - INTERVAL '5' DAY, NOW() - INTERVAL '5' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) as t;

-- 3 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('5.5.5.', id), '/project/nexus-cms', 'Edge', NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) as t;

-- 2 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('6.6.6.', id), '/post/java-21-features', 'Chrome', NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) as t;

-- Yesterday (High volume for comparison)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('7.7.7.', id), '/', 'Mozilla', NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY, 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) as t;

-- Today (Ongoing traffic)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('8.8.8.', id), '/api/v1/public/search/unified', 'Postman', NOW(), NOW(), 0 
FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) as t;
