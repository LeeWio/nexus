-- V5: Create File Metadata Table
CREATE TABLE `sys_file` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name` VARCHAR(100) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(500) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_type` VARCHAR(100) NOT NULL,
    `uploader_id` BIGINT,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    `version` BIGINT DEFAULT 0,
    CONSTRAINT `fk_file_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
