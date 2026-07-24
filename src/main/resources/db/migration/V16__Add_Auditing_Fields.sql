-- V16: Add auditing fields (created_by, last_modified_by) to all relevant tables

ALTER TABLE `sys_role` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `sys_role` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `sys_user` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `sys_user` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `sys_menu` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `sys_menu` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_category` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_category` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_tag` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_tag` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_post` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_post` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_project` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_project` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_moment` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_moment` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_friend_link` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_friend_link` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `sys_file` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `sys_file` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `sys_config` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `sys_config` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `blog_comment` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `blog_comment` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `kanban_column` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `kanban_column` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;

ALTER TABLE `kanban_item` ADD COLUMN `created_by` VARCHAR(50) DEFAULT 'SYSTEM' AFTER `id`;
ALTER TABLE `kanban_item` ADD COLUMN `last_modified_by` VARCHAR(50) AFTER `updated_at`;
