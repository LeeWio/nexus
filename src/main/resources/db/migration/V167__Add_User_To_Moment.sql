-- V167: Add User association to Moment

ALTER TABLE blog_moment ADD COLUMN user_id BIGINT;

-- Match user_id based on created_by matching username or email in sys_user
UPDATE blog_moment bm
JOIN sys_user u ON (bm.created_by = u.username OR bm.created_by = u.email)
SET bm.user_id = u.id;

-- Fallback to the default admin user if user_id is still null
UPDATE blog_moment bm
SET bm.user_id = (SELECT id FROM sys_user WHERE email = 'just.vireo@gmail.com' LIMIT 1)
WHERE bm.user_id IS NULL;

-- Fallback to the first user in case admin is not present
UPDATE blog_moment bm
SET bm.user_id = (SELECT id FROM sys_user ORDER BY id ASC LIMIT 1)
WHERE bm.user_id IS NULL;

-- Add foreign key constraint
ALTER TABLE blog_moment ADD CONSTRAINT fk_moment_user FOREIGN KEY (user_id) REFERENCES sys_user(id);
