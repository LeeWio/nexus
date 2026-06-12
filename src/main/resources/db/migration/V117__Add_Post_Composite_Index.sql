-- V117: Add composite index for blog post public list performance
-- This optimizes queries like: WHERE status = 'PUBLISHED' AND is_featured = true ORDER BY created_at DESC
CREATE INDEX `idx_post_list_stats` ON `blog_post` (`status`, `is_featured`, `created_at`);

-- Also optimize slug lookups if not already explicitly indexed (V1 has UNIQUE which is enough, but double check)
-- V1 has UNIQUE INDEX on slug, so we are good there.
