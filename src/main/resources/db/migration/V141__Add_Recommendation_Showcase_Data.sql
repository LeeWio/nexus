-- V141: Add richer public and personalized recommendation showcase data.

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `email`, `nickname`, `avatar`, `bio`, `status`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
(2, 'editor', '$2a$10$Xm.LhUv3P8nJkG.N0rR6OeWkF/lR.h6hHlKz6B.Q0xY/xS/rS/rS/', 'editor@example.com', 'Nexus Editor', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80', 'Curates product engineering notes and field guides.', 'ACTIVE', NOW(3), NOW(3), 'SYSTEM', 0);

INSERT IGNORE INTO `blog_category` (`name`, `slug`, `description`, `icon`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
('Architecture', 'architecture', 'System design, modularity, scaling, and long-term engineering tradeoffs.', 'Network', NOW(3), NOW(3), 'SYSTEM', 0),
('Product Engineering', 'product-engineering', 'Shipping product features with pragmatic technical decisions.', 'Rocket', NOW(3), NOW(3), 'SYSTEM', 0),
('Design Systems', 'design-systems', 'Reusable UI foundations, tokens, accessibility, and interface craft.', 'Palette', NOW(3), NOW(3), 'SYSTEM', 0);

INSERT IGNORE INTO `blog_tag` (`name`, `slug`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
('Architecture', 'architecture', NOW(3), NOW(3), 'SYSTEM', 0),
('Performance', 'performance', NOW(3), NOW(3), 'SYSTEM', 0),
('Security', 'security', NOW(3), NOW(3), 'SYSTEM', 0),
('Observability', 'observability', NOW(3), NOW(3), 'SYSTEM', 0),
('UX', 'ux', NOW(3), NOW(3), 'SYSTEM', 0),
('Design Systems', 'design-systems', NOW(3), NOW(3), 'SYSTEM', 0),
('Next.js', 'nextjs', NOW(3), NOW(3), 'SYSTEM', 0),
('Recommendation', 'recommendation', NOW(3), NOW(3), 'SYSTEM', 0),
('Content Strategy', 'content-strategy', NOW(3), NOW(3), 'SYSTEM', 0),
('AI', 'ai', NOW(3), NOW(3), 'SYSTEM', 0),
('DevOps', 'devops', NOW(3), NOW(3), 'SYSTEM', 0);

INSERT IGNORE INTO `blog_post`
(`title`, `slug`, `cover_image`, `summary`, `auto_summary`, `content`, `content_type`, `status`, `is_featured`, `views`, `likes_count`, `favorites_count`, `word_count`, `reading_time_minutes`, `category_id`, `author_id`, `published_at`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES
('Designing a Recommendation Surface That Does Not Feel Random',
 'designing-recommendation-surface',
 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1400&q=80',
 'A practical model for editorial picks, personalized suggestions, and contextual read-next blocks.',
 'Recommendation modules work best when each section has a clear job and a distinct scoring strategy.',
 '# Designing a Recommendation Surface\n\nA strong recommendation experience separates editorial judgment, personal preference, and current reading context. This guide walks through the ranking signals and product rules that keep every section useful.',
 'MDX', 'PUBLISHED', 1, 1840, 142, 78, 1320, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'product-engineering'), 2,
 DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3), 'SYSTEM', 0),

('Spring Boot API Boundaries for Content Heavy Products',
 'spring-boot-api-boundaries-content-products',
 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80',
 'How to keep public reading APIs, admin workflows, and personalization endpoints cleanly separated.',
 'Content APIs stay maintainable when public discovery, user library, and editorial workflow each own their contract.',
 '# Spring Boot API Boundaries\n\nThis article outlines controller and service boundaries for a content product, including DTO shape, cache invalidation, and transactional rules.',
 'MDX', 'PUBLISHED', 1, 1630, 118, 64, 1680, 9,
 (SELECT id FROM `blog_category` WHERE slug = 'backend'), 1,
 DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3), 'SYSTEM', 0),

('Full Stack Feature Flags Without Losing Product Clarity',
 'full-stack-feature-flags-product-clarity',
 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1400&q=80',
 'A compact approach to rolling out UI and backend changes while keeping behavior understandable.',
 'Feature flags should protect releases without turning product behavior into a mystery.',
 '# Full Stack Feature Flags\n\nFlags help teams ship carefully, but the model needs ownership, expiry, and observable rollout states.',
 'MDX', 'PUBLISHED', 0, 980, 71, 38, 1180, 6,
 (SELECT id FROM `blog_category` WHERE slug = 'product-engineering'), 2,
 DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3), 'SYSTEM', 0),

('A Field Guide to Read Next Ranking',
 'field-guide-read-next-ranking',
 'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=1400&q=80',
 'Read-next systems need local context first: series, tags, category, then popularity as a fallback.',
 'Contextual recommendations feel intentional when the source article controls the candidate pool.',
 '# A Field Guide to Read Next Ranking\n\nRead-next ranking starts with the article in hand. Series order, shared tags, category overlap, and freshness create a useful next step.',
 'MDX', 'PUBLISHED', 1, 1420, 96, 52, 1260, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'architecture'), 1,
 DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3), 'SYSTEM', 0),

('Measuring Frontend Perceived Performance',
 'measuring-frontend-perceived-performance',
 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=80',
 'Beyond raw timing metrics: visual stability, interaction delay, and content readiness.',
 'Performance work should explain what users perceive, not only what the server reports.',
 '# Measuring Frontend Perceived Performance\n\nPerceived performance combines content timing, interaction readiness, animation cost, and layout stability.',
 'MDX', 'PUBLISHED', 0, 1210, 89, 41, 1540, 8,
 (SELECT id FROM `blog_category` WHERE slug = 'frontend'), 2,
 DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 7 DAY), NOW(3), 'SYSTEM', 0),

('Design Tokens That Survive Real Product Screens',
 'design-tokens-real-product-screens',
 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?auto=format&fit=crop&w=1400&q=80',
 'How to define tokens for dense dashboards, article cards, and theme variants.',
 'Tokens are useful only when they survive real density, mixed content, and responsive constraints.',
 '# Design Tokens That Survive\n\nThis post covers color roles, typography rhythm, spacing, and component-level constraints for product screens.',
 'MDX', 'PUBLISHED', 1, 1515, 103, 59, 1390, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'design-systems'), 2,
 DATE_SUB(NOW(3), INTERVAL 8 DAY), DATE_SUB(NOW(3), INTERVAL 8 DAY), NOW(3), 'SYSTEM', 0),

('Building Search Facets That Help Readers Browse',
 'building-search-facets-readers-browse',
 'https://images.unsplash.com/photo-1487058792275-0ad4aaf24ca7?auto=format&fit=crop&w=1400&q=80',
 'Categories, tags, archive months, and content types can make a blog feel explorable.',
 'Faceted navigation turns a flat archive into a browsable content system.',
 '# Building Search Facets\n\nGood facets expose useful dimensions without overwhelming the reader. This guide covers ordering, counts, and empty states.',
 'MDX', 'PUBLISHED', 0, 875, 54, 27, 1010, 5,
 (SELECT id FROM `blog_category` WHERE slug = 'backend'), 1,
 DATE_SUB(NOW(3), INTERVAL 10 DAY), DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3), 'SYSTEM', 0),

('Database Indexes for Public Content Feeds',
 'database-indexes-public-content-feeds',
 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=1400&q=80',
 'Indexes that support published lists, archive pages, category browsing, and ranking pools.',
 'Public feeds become predictable when status, publication time, and soft deletion are indexed together.',
 '# Database Indexes for Public Content Feeds\n\nThis article explains how content feeds use composite indexes and bounded candidate pools.',
 'MDX', 'PUBLISHED', 0, 1135, 67, 34, 1470, 8,
 (SELECT id FROM `blog_category` WHERE slug = 'database'), 1,
 DATE_SUB(NOW(3), INTERVAL 12 DAY), DATE_SUB(NOW(3), INTERVAL 12 DAY), NOW(3), 'SYSTEM', 0),

('Caching Strategy for Blog Discovery APIs',
 'caching-strategy-blog-discovery-apis',
 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1400&q=80',
 'A cache plan for spotlight, curated posts, latest lists, and SEO metadata.',
 'Discovery APIs need coarse cache invalidation tied to publication and editorial changes.',
 '# Caching Strategy for Blog Discovery APIs\n\nThis guide covers cache keys, invalidation events, and when not to cache user-specific recommendation data.',
 'MDX', 'PUBLISHED', 1, 1370, 82, 46, 1220, 6,
 (SELECT id FROM `blog_category` WHERE slug = 'architecture'), 1,
 DATE_SUB(NOW(3), INTERVAL 14 DAY), DATE_SUB(NOW(3), INTERVAL 14 DAY), NOW(3), 'SYSTEM', 0),

('Observability for Content Workflows',
 'observability-content-workflows',
 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1400&q=80',
 'Trace publication, webhook delivery, cache refresh, and recommendation fallbacks.',
 'Content workflows need observable state transitions so editors can trust automation.',
 '# Observability for Content Workflows\n\nLogs, metrics, and audit fields make asynchronous content workflows debuggable.',
 'MDX', 'PUBLISHED', 0, 990, 61, 31, 1350, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'devops'), 2,
 DATE_SUB(NOW(3), INTERVAL 16 DAY), DATE_SUB(NOW(3), INTERVAL 16 DAY), NOW(3), 'SYSTEM', 0),

('Secure Webhooks in a Content Platform',
 'secure-webhooks-content-platform',
 'https://images.unsplash.com/photo-1510511459019-5dda7724fd87?auto=format&fit=crop&w=1400&q=80',
 'Outbound URL validation, retry boundaries, and event payload hygiene for webhooks.',
 'Webhook systems need strict destination validation and observable delivery behavior.',
 '# Secure Webhooks in a Content Platform\n\nThis article covers SSRF safeguards, retries, idempotency, and admin-facing delivery state.',
 'MDX', 'PUBLISHED', 0, 1188, 76, 37, 1490, 8,
 (SELECT id FROM `blog_category` WHERE slug = 'backend'), 1,
 DATE_SUB(NOW(3), INTERVAL 18 DAY), DATE_SUB(NOW(3), INTERVAL 18 DAY), NOW(3), 'SYSTEM', 0),

('AI Assisted Editorial Review That Keeps Humans in Control',
 'ai-assisted-editorial-review-human-control',
 'https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=1400&q=80',
 'Use AI to summarize, classify, and flag content without replacing editorial decisions.',
 'AI is most useful in editorial systems when it explains suggestions and leaves approval to people.',
 '# AI Assisted Editorial Review\n\nThis post covers AI-generated summaries, classification, sensitive-word review, and transparent editor controls.',
 'MDX', 'PUBLISHED', 1, 1328, 91, 44, 1580, 8,
 (SELECT id FROM `blog_category` WHERE slug = 'ai'), 2,
 DATE_SUB(NOW(3), INTERVAL 20 DAY), DATE_SUB(NOW(3), INTERVAL 20 DAY), NOW(3), 'SYSTEM', 0),

('Newsletter Picks from a Living Archive',
 'newsletter-picks-living-archive',
 'https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=1400&q=80',
 'Turn older evergreen posts into weekly newsletter recommendations.',
 'A strong archive can keep producing useful recommendations long after publication.',
 '# Newsletter Picks from a Living Archive\n\nThis guide shows how archive facets, engagement, and freshness windows shape newsletter content.',
 'MDX', 'PUBLISHED', 0, 760, 43, 24, 1050, 5,
 (SELECT id FROM `blog_category` WHERE slug = 'product-engineering'), 2,
 DATE_SUB(NOW(3), INTERVAL 24 DAY), DATE_SUB(NOW(3), INTERVAL 24 DAY), NOW(3), 'SYSTEM', 0),

('CSS Scope Boundaries for Theme Rich Interfaces',
 'css-scope-boundaries-theme-rich-interfaces',
 'https://images.unsplash.com/photo-1559028012-481c04fa702d?auto=format&fit=crop&w=1400&q=80',
 'Theme variants need strict scope boundaries so typography and spacing do not leak.',
 'Theme systems fail quietly when typography and component spacing leak across boundaries.',
 '# CSS Scope Boundaries\n\nThis article covers root attributes, scoped vendor CSS, and visual regression checks for rich theme systems.',
 'MDX', 'PUBLISHED', 0, 1045, 74, 39, 1440, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'design-systems'), 2,
 DATE_SUB(NOW(3), INTERVAL 28 DAY), DATE_SUB(NOW(3), INTERVAL 28 DAY), NOW(3), 'SYSTEM', 0),

('A Practical Guide to Article Card Density',
 'practical-guide-article-card-density',
 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1400&q=80',
 'Make article grids scan well with compact metadata, clear covers, and stable text rhythm.',
 'Card density matters when several recommendation sections share the same viewport.',
 '# A Practical Guide to Article Card Density\n\nThis article looks at title length, summaries, metadata, cover ratios, and responsive card grids.',
 'MDX', 'PUBLISHED', 0, 915, 58, 29, 980, 5,
 (SELECT id FROM `blog_category` WHERE slug = 'design-systems'), 2,
 DATE_SUB(NOW(3), INTERVAL 32 DAY), DATE_SUB(NOW(3), INTERVAL 32 DAY), NOW(3), 'SYSTEM', 0),

('Redis Patterns for Reader Interaction Counters',
 'redis-patterns-reader-interaction-counters',
 'https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=1400&q=80',
 'Batch view counters, like counters, and cache-friendly interaction reads.',
 'Reader interaction counters benefit from fast writes and predictable sync boundaries.',
 '# Redis Patterns for Reader Interaction Counters\n\nThis post explains how to batch high-volume counters while keeping public pages fresh.',
 'MDX', 'PUBLISHED', 0, 1090, 66, 35, 1210, 6,
 (SELECT id FROM `blog_category` WHERE slug = 'database'), 1,
 DATE_SUB(NOW(3), INTERVAL 36 DAY), DATE_SUB(NOW(3), INTERVAL 36 DAY), NOW(3), 'SYSTEM', 0),

('Kubernetes Deployments for a Small Content Team',
 'kubernetes-deployments-small-content-team',
 'https://images.unsplash.com/photo-1667372393119-3d4c48d07fc9?auto=format&fit=crop&w=1400&q=80',
 'A lean deployment model with health checks, migrations, and rollback visibility.',
 'Small teams need deployment flows that are boring, visible, and easy to recover.',
 '# Kubernetes Deployments for a Small Content Team\n\nThis guide covers image promotion, readiness checks, Flyway migrations, and rollback habits.',
 'MDX', 'PUBLISHED', 0, 845, 47, 23, 1380, 7,
 (SELECT id FROM `blog_category` WHERE slug = 'devops'), 1,
 DATE_SUB(NOW(3), INTERVAL 40 DAY), DATE_SUB(NOW(3), INTERVAL 40 DAY), NOW(3), 'SYSTEM', 0),

('Building a Calm Admin Dashboard for Editors',
 'building-calm-admin-dashboard-editors',
 'https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=1400&q=80',
 'Editorial tools should emphasize workflow state, review quality, and low-friction navigation.',
 'Admin dashboards are better when they are quiet, dense, and grounded in real tasks.',
 '# Building a Calm Admin Dashboard\n\nThis article walks through editorial status, review queues, audit metadata, and navigation patterns.',
 'MDX', 'PUBLISHED', 0, 1240, 83, 48, 1520, 8,
 (SELECT id FROM `blog_category` WHERE slug = 'product-engineering'), 2,
 DATE_SUB(NOW(3), INTERVAL 45 DAY), DATE_SUB(NOW(3), INTERVAL 45 DAY), NOW(3), 'SYSTEM', 0);

UPDATE `blog_post`
SET `path` = CONCAT('/', `slug`, '/')
WHERE `path` IS NULL AND `slug` IN (
    'designing-recommendation-surface',
    'spring-boot-api-boundaries-content-products',
    'full-stack-feature-flags-product-clarity',
    'field-guide-read-next-ranking',
    'measuring-frontend-perceived-performance',
    'design-tokens-real-product-screens',
    'building-search-facets-readers-browse',
    'database-indexes-public-content-feeds',
    'caching-strategy-blog-discovery-apis',
    'observability-content-workflows',
    'secure-webhooks-content-platform',
    'ai-assisted-editorial-review-human-control',
    'newsletter-picks-living-archive',
    'css-scope-boundaries-theme-rich-interfaces',
    'practical-guide-article-card-density',
    'redis-patterns-reader-interaction-counters',
    'kubernetes-deployments-small-content-team',
    'building-calm-admin-dashboard-editors'
);

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('recommendation', 'content-strategy', 'ux')
WHERE p.slug = 'designing-recommendation-surface';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('spring-boot', 'architecture', 'java')
WHERE p.slug = 'spring-boot-api-boundaries-content-products';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('architecture', 'devlog', 'ux')
WHERE p.slug = 'full-stack-feature-flags-product-clarity';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('recommendation', 'architecture', 'content-strategy')
WHERE p.slug = 'field-guide-read-next-ranking';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('react', 'nextjs', 'performance')
WHERE p.slug = 'measuring-frontend-perceived-performance';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('design-systems', 'ux', 'performance')
WHERE p.slug = 'design-tokens-real-product-screens';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('spring-boot', 'mysql', 'architecture')
WHERE p.slug = 'building-search-facets-readers-browse';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('mysql', 'postgresql', 'performance')
WHERE p.slug = 'database-indexes-public-content-feeds';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('redis', 'performance', 'architecture')
WHERE p.slug = 'caching-strategy-blog-discovery-apis';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('observability', 'devops', 'architecture')
WHERE p.slug = 'observability-content-workflows';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('security', 'spring-boot', 'architecture')
WHERE p.slug = 'secure-webhooks-content-platform';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('ai', 'content-strategy', 'recommendation')
WHERE p.slug = 'ai-assisted-editorial-review-human-control';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('content-strategy', 'recommendation', 'devlog')
WHERE p.slug = 'newsletter-picks-living-archive';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('design-systems', 'ux', 'nextjs')
WHERE p.slug = 'css-scope-boundaries-theme-rich-interfaces';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('design-systems', 'ux', 'content-strategy')
WHERE p.slug = 'practical-guide-article-card-density';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('redis', 'performance', 'spring-boot')
WHERE p.slug = 'redis-patterns-reader-interaction-counters';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('kubernetes', 'docker', 'devops')
WHERE p.slug = 'kubernetes-deployments-small-content-team';

INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id
FROM `blog_post` p
JOIN `blog_tag` t ON t.slug IN ('ux', 'content-strategy', 'design-systems')
WHERE p.slug = 'building-calm-admin-dashboard-editors';

INSERT IGNORE INTO `blog_category_follow` (`user_id`, `category_id`, `created_by`, `created_at`, `updated_at`, `is_deleted`)
SELECT 1, c.id, 'SYSTEM', NOW(3), NOW(3), 0
FROM `blog_category` c
WHERE c.slug IN ('frontend', 'backend', 'architecture', 'product-engineering', 'design-systems');

INSERT IGNORE INTO `blog_reading_history`
(`user_id`, `post_id`, `progress_percent`, `position_anchor`, `last_read_at`, `created_by`, `created_at`, `updated_at`, `is_deleted`)
SELECT 1, p.id, 72, 'section-api-boundaries', DATE_SUB(NOW(3), INTERVAL 2 HOUR), 'SYSTEM', DATE_SUB(NOW(3), INTERVAL 2 HOUR), NOW(3), 0
FROM `blog_post` p
WHERE p.slug = 'spring-boot-api-boundaries-content-products';

INSERT IGNORE INTO `blog_reading_history`
(`user_id`, `post_id`, `progress_percent`, `position_anchor`, `last_read_at`, `created_by`, `created_at`, `updated_at`, `is_deleted`)
SELECT 1, p.id, 46, 'section-visual-stability', DATE_SUB(NOW(3), INTERVAL 1 DAY), 'SYSTEM', DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3), 0
FROM `blog_post` p
WHERE p.slug = 'measuring-frontend-perceived-performance';

INSERT IGNORE INTO `blog_reading_history`
(`user_id`, `post_id`, `progress_percent`, `position_anchor`, `last_read_at`, `created_by`, `created_at`, `updated_at`, `is_deleted`)
SELECT 1, p.id, 100, 'done', DATE_SUB(NOW(3), INTERVAL 3 DAY), 'SYSTEM', DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3), 0
FROM `blog_post` p
WHERE p.slug = 'design-tokens-real-product-screens';

INSERT IGNORE INTO `blog_post_favorite` (`post_id`, `user_id`, `created_at`)
SELECT p.id, 1, DATE_SUB(NOW(3), INTERVAL 4 HOUR)
FROM `blog_post` p
WHERE p.slug IN ('design-tokens-real-product-screens', 'caching-strategy-blog-discovery-apis', 'building-calm-admin-dashboard-editors');

INSERT IGNORE INTO `blog_post_like` (`post_id`, `user_id`, `created_at`)
SELECT p.id, 1, DATE_SUB(NOW(3), INTERVAL 3 HOUR)
FROM `blog_post` p
WHERE p.slug IN ('designing-recommendation-surface', 'field-guide-read-next-ranking', 'ai-assisted-editorial-review-human-control');
