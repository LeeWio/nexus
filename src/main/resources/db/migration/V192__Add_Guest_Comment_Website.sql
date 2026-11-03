-- Optional public website for a guest comment. Account websites stay on sys_user.
SET @guest_website_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'blog_comment' AND column_name = 'guest_website'
);
SET @sql := IF(@guest_website_exists = 0,
    'ALTER TABLE blog_comment ADD COLUMN guest_website VARCHAR(100) NULL AFTER guest_email',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
