-- V28: Add slug column to blog_project table

ALTER TABLE `blog_project` ADD COLUMN `slug` VARCHAR(100) AFTER `name`;

-- Update existing records with a default slug if any (id as slug)
UPDATE `blog_project` SET `slug` = CAST(id AS CHAR) WHERE `slug` IS NULL;

-- Make it unique and not null
ALTER TABLE `blog_project` MODIFY COLUMN `slug` VARCHAR(100) NOT NULL UNIQUE;
