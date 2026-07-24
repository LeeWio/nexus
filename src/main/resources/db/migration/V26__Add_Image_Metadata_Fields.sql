ALTER TABLE `sys_file` ADD COLUMN `thumbnail_url` VARCHAR(500) DEFAULT NULL AFTER `file_url`;
ALTER TABLE `sys_file` ADD COLUMN `width` INT DEFAULT NULL AFTER `thumbnail_url`;
ALTER TABLE `sys_file` ADD COLUMN `height` INT DEFAULT NULL AFTER `width`;
