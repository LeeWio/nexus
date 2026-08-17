-- Add the new visibility column with default value 'PUBLIC'
ALTER TABLE blog_moment ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Migrate existing is_published column data to visibility column
UPDATE blog_moment SET visibility = 'PRIVATE' WHERE is_published = FALSE;

-- Drop the old is_published column
ALTER TABLE blog_moment DROP COLUMN is_published;
