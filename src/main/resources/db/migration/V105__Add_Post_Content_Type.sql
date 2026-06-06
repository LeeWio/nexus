-- V105: Add support for multiple content formats (JSON, MDX)

-- Add content_type to blog_post table
ALTER TABLE `blog_post` ADD COLUMN `content_type` VARCHAR(20) NOT NULL DEFAULT 'JSON' AFTER `content`;

-- Add content_type to blog_post_revision table
ALTER TABLE `blog_post_revision` ADD COLUMN `content_type` VARCHAR(20) NOT NULL DEFAULT 'JSON' AFTER `content`;
