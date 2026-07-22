-- V142: Ensure related posts data integrity by recovering categories and tags bypassed by baselining

-- 1. Insert missing core categories (ignored if already existing)
INSERT IGNORE INTO `blog_category` (`name`, `slug`, `description`, `icon`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
('Backend', 'backend', 'Backend development, servers, databases, and APIs.', 'Server', NOW(3), NOW(3), 'SYSTEM', 0),
('Frontend', 'frontend', 'Frontend development, UI/UX, and modern web technologies.', 'Layout', NOW(3), NOW(3), 'SYSTEM', 0),
('Database', 'database', 'Database design, SQL optimization, caching, and storage.', 'Database', NOW(3), NOW(3), 'SYSTEM', 0),
('DevOps', 'devops', 'Continuous integration, deployment workflows, pipelines, and cloud systems.', 'Cpu', NOW(3), NOW(3), 'SYSTEM', 0),
('Artificial Intelligence', 'ai', 'Artificial Intelligence, machine learning, and content analysis.', 'Brain', NOW(3), NOW(3), 'SYSTEM', 0),
('Technology', 'technology', 'Software engineering, general technology trends, and coding.', 'Laptop', NOW(3), NOW(3), 'SYSTEM', 0);

-- 2. Insert missing core tags (ignored if already existing)
INSERT IGNORE INTO `blog_tag` (`name`, `slug`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
('Java', 'java', NOW(3), NOW(3), 'SYSTEM', 0),
('Spring Boot', 'spring-boot', NOW(3), NOW(3), 'SYSTEM', 0),
('React', 'react', NOW(3), NOW(3), 'SYSTEM', 0),
('DevLog', 'devlog', NOW(3), NOW(3), 'SYSTEM', 0),
('Docker', 'docker', NOW(3), NOW(3), 'SYSTEM', 0),
('Kubernetes', 'kubernetes', NOW(3), NOW(3), 'SYSTEM', 0),
('MySQL', 'mysql', NOW(3), NOW(3), 'SYSTEM', 0),
('PostgreSQL', 'postgresql', NOW(3), NOW(3), 'SYSTEM', 0),
('Redis', 'redis', NOW(3), NOW(3), 'SYSTEM', 0);

-- 3. Update posts to associate with their proper category_id
UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'backend')
WHERE `slug` IN ('spring-boot-api-boundaries-content-products', 'building-search-facets-readers-browse', 'secure-webhooks-content-platform')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'backend'));

UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'frontend')
WHERE `slug` IN ('measuring-frontend-perceived-performance')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'frontend'));

UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'database')
WHERE `slug` IN ('database-indexes-public-content-feeds', 'redis-patterns-reader-interaction-counters')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'database'));

UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'devops')
WHERE `slug` IN ('observability-content-workflows', 'kubernetes-deployments-small-content-team')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'devops'));

UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'ai')
WHERE `slug` IN ('ai-assisted-editorial-review-human-control')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'ai'));

-- Ensure Technology posts are also linked
UPDATE `blog_post` SET `category_id` = (SELECT id FROM `blog_category` WHERE slug = 'technology')
WHERE `slug` IN ('welcome-to-nexus', 'java-21-features', 'modern-frontend-react', 'building-search-elasticsearch')
  AND (`category_id` IS NULL OR `category_id` NOT IN (SELECT id FROM `blog_category` WHERE slug = 'technology'));

-- 4. Ensure proper tag associations are populated for all posts
-- spring-boot-api-boundaries-content-products
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('spring-boot', 'architecture', 'java')
WHERE p.slug = 'spring-boot-api-boundaries-content-products';

-- full-stack-feature-flags-product-clarity
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('architecture', 'devlog', 'ux')
WHERE p.slug = 'full-stack-feature-flags-product-clarity';

-- measuring-frontend-perceived-performance
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('react', 'nextjs', 'performance')
WHERE p.slug = 'measuring-frontend-perceived-performance';

-- building-search-facets-readers-browse
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('spring-boot', 'mysql', 'architecture')
WHERE p.slug = 'building-search-facets-readers-browse';

-- database-indexes-public-content-feeds
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('mysql', 'postgresql', 'performance')
WHERE p.slug = 'database-indexes-public-content-feeds';

-- caching-strategy-blog-discovery-apis
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('redis', 'performance', 'architecture')
WHERE p.slug = 'caching-strategy-blog-discovery-apis';

-- secure-webhooks-content-platform
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('security', 'spring-boot', 'architecture')
WHERE p.slug = 'secure-webhooks-content-platform';

-- redis-patterns-reader-interaction-counters
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('redis', 'performance', 'spring-boot')
WHERE p.slug = 'redis-patterns-reader-interaction-counters';

-- kubernetes-deployments-small-content-team
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('kubernetes', 'docker', 'devops')
WHERE p.slug = 'kubernetes-deployments-small-content-team';

-- welcome-to-nexus
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('devlog')
WHERE p.slug = 'welcome-to-nexus';

-- java-21-features
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('java')
WHERE p.slug = 'java-21-features';

-- modern-frontend-react
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('react')
WHERE p.slug = 'modern-frontend-react';

-- building-search-elasticsearch
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('spring-boot', 'java')
WHERE p.slug = 'building-search-elasticsearch';
