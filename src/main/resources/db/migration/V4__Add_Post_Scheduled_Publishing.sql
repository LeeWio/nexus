-- V4: Add Scheduled Publishing support for Posts
ALTER TABLE `blog_post` ADD COLUMN `published_at` DATETIME AFTER `is_featured`;
