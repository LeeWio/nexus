-- V116: Add composite indexes for visit log performance optimization
-- 1. Optimize PV/UV count by range
CREATE INDEX `idx_visit_time_ip` ON `sys_visit_log` (`visit_time`, `ip_address`);

-- 2. Optimize Top Content queries (filter by time, group by url)
CREATE INDEX `idx_visit_time_url` ON `sys_visit_log` (`visit_time`, `request_url`);

-- 3. Optimize Device/Source stats
CREATE INDEX `idx_visit_time_os` ON `sys_visit_log` (`visit_time`, `os`);
CREATE INDEX `idx_visit_time_referer` ON `sys_visit_log` (`visit_time`, `referer`);
