-- V20: Add GitHub metrics fields to blog_project table

ALTER TABLE `blog_project` ADD COLUMN `stars_count` INT DEFAULT 0 AFTER `tech_stack`;
ALTER TABLE `blog_project` ADD COLUMN `forks_count` INT DEFAULT 0 AFTER `stars_count`;
ALTER TABLE `blog_project` ADD COLUMN `language` VARCHAR(50) AFTER `forks_count`;
ALTER TABLE `blog_project` ADD COLUMN `repo_name` VARCHAR(100) AFTER `language`;

-- Index for repo_name to speed up sync lookups
CREATE INDEX `idx_project_repo` ON `blog_project` (`repo_name`);
