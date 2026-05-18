-- V18: Add is_deleted field to all relevant tables for soft delete support

ALTER TABLE `blog_post` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_comment` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `kanban_item` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_category` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_tag` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_project` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_moment` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sys_user` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sys_role` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sys_menu` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sys_file` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `sys_config` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `kanban_column` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `blog_friend_link` ADD COLUMN `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE;

-- Add indices for performance on soft-deleted queries
CREATE INDEX `idx_post_deleted` ON `blog_post` (`is_deleted`);
CREATE INDEX `idx_comment_deleted` ON `blog_comment` (`is_deleted`);
CREATE INDEX `idx_kanban_item_deleted` ON `kanban_item` (`is_deleted`);
