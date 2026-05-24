-- V31: Refine PV/UV balance and extend to a full 14-day high-fidelity trend

-- Clean up ALL previous logs to start fresh with a balanced trend
TRUNCATE TABLE `sys_visit_log`;

-- 14 days ago (Low)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) VALUES 
('10.0.1.1', '/', 'Mozilla', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 0),
('10.0.1.1', '/post/welcome', 'Mozilla', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 0),
('10.0.1.2', '/', 'Mozilla', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 0),
('10.0.1.3', '/post/java-21', 'Mozilla', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), 0);

-- 13 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.2.', id), '/post/welcome', 'Chrome', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) as t;

-- 12 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.3.', id), '/', 'Safari', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) as t;

-- 11 days ago (Growth)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.4.', id), '/post/react-tips', 'Mozilla', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) as t;

-- 10 days ago (Spike - PV 20, UV 12)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.5.', id), '/post/java-21', 'Chrome', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) as t;
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.5.', id), '/project/nexus', 'Chrome', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) as t;

-- 9 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.6.', id), '/', 'Edge', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) as t;

-- 8 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.7.', id), '/post/welcome', 'Mozilla', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) as t;

-- 7 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.8.', id), '/post/java-21', 'Safari', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) as t;

-- 6 days ago (Dip)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.9.', id), '/', 'Chrome', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) as t;

-- 5 days ago (Major Spike - PV 25, UV 15)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.10.', id), '/post/elasticsearch', 'Mozilla', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) as t;
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.10.', id), '/api/v1/public/search/unified', 'Mozilla', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) as t;

-- 4 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.11.', id), '/post/react-tips', 'Chrome', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) as t;

-- 3 days ago
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.12.', id), '/project/starry-weather', 'Safari', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) as t;

-- Yesterday (High Comparison - PV 30, UV 18)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.13.', id), '/', 'Mozilla', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18) as t;
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.13.', id), '/post/welcome', 'Mozilla', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) as t;

-- Today (Live Feel - PV 22, UV 14)
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.14.', id), '/api/v1/public/search/unified', 'Chrome', NOW(), NOW(), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) as t;
INSERT INTO `sys_visit_log` (ip_address, request_url, user_agent, visit_time, created_at, is_deleted) 
SELECT CONCAT('10.0.14.', id), '/post/java-21', 'Chrome', NOW(), NOW(), 0 FROM (SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) as t;
