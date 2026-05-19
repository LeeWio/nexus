ALTER TABLE `sys_file` 
ADD COLUMN `thumbnail_url` VARCHAR(500) DEFAULT NULL AFTER `file_url`,
ADD COLUMN `width` INT DEFAULT NULL AFTER `thumbnail_url`,
ADD COLUMN `height` INT DEFAULT NULL AFTER `width`;
