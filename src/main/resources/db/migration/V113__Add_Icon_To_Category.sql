-- V113: Add Icon Column to Category Table
ALTER TABLE `blog_category` ADD COLUMN `icon` VARCHAR(100) DEFAULT NULL AFTER `description`;
