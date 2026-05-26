-- Add path column for path enumeration tree modeling
ALTER TABLE blog_comment ADD COLUMN path VARCHAR(1000) DEFAULT NULL;

-- Since this is a migration, we should ideally populate the path column.
-- For a simple 1-level deep migration if parents are currently null or 1 level deep:
UPDATE blog_comment SET path = CONCAT('/', id, '/') WHERE parent_id IS NULL;

-- Assuming most existing comments in a blog aren't too deep, we can do a couple of updates for existing data
UPDATE blog_comment c1 
JOIN blog_comment c2 ON c1.parent_id = c2.id 
SET c1.path = CONCAT(c2.path, c1.id, '/') 
WHERE c1.parent_id IS NOT NULL AND c2.path IS NOT NULL;
