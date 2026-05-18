CREATE TABLE `kanban_column` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `color` VARCHAR(50),
    `order_index` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `kanban_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT,
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    `order_index` INT NOT NULL DEFAULT 0,
    `reminder_at` DATETIME,
    `column_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    CONSTRAINT `fk_kanban_item_column` FOREIGN KEY (`column_id`) REFERENCES `kanban_column` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `kanban_item_tag` (
    `item_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    PRIMARY KEY (`item_id`, `tag_id`),
    CONSTRAINT `fk_kanban_item_tag_item` FOREIGN KEY (`item_id`) REFERENCES `kanban_item` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kanban_item_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
