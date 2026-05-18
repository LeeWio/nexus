CREATE TABLE `sys_visit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ip_address` VARCHAR(50) NOT NULL,
    `location` VARCHAR(100),
    `user_agent` VARCHAR(500),
    `browser` VARCHAR(50),
    `os` VARCHAR(50),
    `request_url` VARCHAR(255) NOT NULL,
    `referer` VARCHAR(255),
    `visit_time` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    `created_by` VARCHAR(50) DEFAULT 'SYSTEM',
    `last_modified_by` VARCHAR(50),
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX `idx_visit_time` (`visit_time`),
    INDEX `idx_request_url` (`request_url`),
    INDEX `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
