CREATE TABLE `blog_series` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `created_by` VARCHAR(50) DEFAULT 'SYSTEM',
    `name` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `cover_image` VARCHAR(255),
    `is_published` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    `last_modified_by` VARCHAR(50),
    UNIQUE KEY `uk_series_slug` (`slug`),
    INDEX `idx_series_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `blog_post` ADD COLUMN `series_id` BIGINT AFTER `category_id`;
ALTER TABLE `blog_post` ADD COLUMN `series_order` INT DEFAULT 0 AFTER `series_id`;
ALTER TABLE `blog_post` ADD CONSTRAINT `fk_post_series` FOREIGN KEY (`series_id`) REFERENCES `blog_series` (`id`) ON DELETE SET NULL;
