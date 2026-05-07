-- V2: Add Comment System
CREATE TABLE `blog_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,
    `ip_address` VARCHAR(50),
    `user_agent` VARCHAR(255),
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    `version` BIGINT DEFAULT 0,
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `blog_comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_comment_post` ON `blog_comment` (`post_id`);
CREATE INDEX `idx_comment_status` ON `blog_comment` (`status`);
