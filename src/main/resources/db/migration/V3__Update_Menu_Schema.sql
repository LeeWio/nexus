-- V3: Update sys_menu schema to match Menu entity
ALTER TABLE `sys_menu` ADD COLUMN `permission` VARCHAR(100) AFTER `path`;
ALTER TABLE `sys_menu` ADD COLUMN `type` INT NOT NULL DEFAULT 0 AFTER `permission`;
ALTER TABLE `sys_menu` CHANGE COLUMN `sort` `sort_order` INT DEFAULT 0;
ALTER TABLE `sys_menu` MODIFY COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0;
