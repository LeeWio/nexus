-- V27: Add sample data for frontend debugging

-- 1. Ensure at least one user exists
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `email`, `nickname`, `status`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES (1, 'admin', '$2a$10$Xm.LhUv3P8nJkG.N0rR6OeWkF/lR.h6hHlKz6B.Q0xY/xS/rS/rS/', 'admin@example.com', 'Nexus Admin', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 0);

-- 2. Add some categories
INSERT IGNORE INTO `blog_category` (`id`, `name`, `slug`, `description`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
(1, 'Technology', 'technology', 'All things about software engineering and tech.', NOW(), NOW(), 'SYSTEM', 0),
(2, 'Life', 'life', 'Personal stories and life reflections.', NOW(), NOW(), 'SYSTEM', 0),
(3, 'Travel', 'travel', 'Travel journals and photography.', NOW(), NOW(), 'SYSTEM', 0);

-- 3. Add some tags
INSERT IGNORE INTO `blog_tag` (`id`, `name`, `slug`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
(1, 'Java', 'java', NOW(), NOW(), 'SYSTEM', 0),
(2, 'Spring Boot', 'spring-boot', NOW(), NOW(), 'SYSTEM', 0),
(3, 'React', 'react', NOW(), NOW(), 'SYSTEM', 0),
(4, 'DevLog', 'devlog', NOW(), NOW(), 'SYSTEM', 0);

-- 4. Add sample blog posts
INSERT IGNORE INTO `blog_post` (`title`, `slug`, `summary`, `content`, `status`, `is_featured`, `views`, `likes_count`, `favorites_count`, `category_id`, `author_id`, `published_at`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
('Welcome to Nexus Blog', 'welcome-to-nexus', 'This is your first post in the Nexus blog system.', '# Welcome to Nexus\n\nThis is a sample post to help you get started with your new blog. Nexus is built with:\n\n- **Java 21**\n- **Spring Boot 3**\n- **MySQL**\n- **Redis**\n\nYou can edit or delete this post in the admin dashboard.', 'PUBLISHED', 1, 125, 0, 0, 1, 1, NOW(), NOW(), NOW(), 'SYSTEM', 0),

('Exploring Java 21 Features', 'java-21-features', 'A deep dive into the latest features of Java 21 including Virtual Threads and Pattern Matching.', '# Java 21 New Features\n\nJava 21 is a Long-Term Support (LTS) release. Key features include:\n\n## Virtual Threads\nVirtual threads are lightweight threads that dramatically reduce the effort of writing, maintaining, and observing high-throughput concurrent applications.\n\n## Record Patterns\nEnhance the Java programming language with record patterns to deconstruct record values.\n\n... more content ...', 'PUBLISHED', 0, 450, 0, 0, 1, 1, NOW(), NOW(), NOW(), 'SYSTEM', 0),

('Modern Frontend with React and Tailwind', 'modern-frontend-react', 'How to build beautiful and responsive user interfaces with React and Tailwind CSS.', '# Modern Frontend\n\nBuilding UIs has never been easier. With React for logic and Tailwind for styling, you can create stunning applications in no time.\n\n### Why Tailwind?\n\n- Utility-first approach\n- Responsive design by default\n- Highly customizable', 'PUBLISHED', 0, 210, 0, 0, 1, 1, NOW(), NOW(), NOW(), 'SYSTEM', 0),

('A Weekend in the Mountains', 'weekend-in-mountains', 'My recent trip to the Alps was breathtaking. Here are some highlights.', '# Alps Trip\n\nLast weekend, I went to the mountains. The air was fresh, and the view was incredible.\n\n![Alps](https://images.unsplash.com/photo-1464822759023-fed622ff2c3b)\n\nI spent three days hiking and taking photos. It was the perfect escape from the city.', 'PUBLISHED', 0, 85, 0, 0, 3, 1, NOW(), NOW(), NOW(), 'SYSTEM', 0),

('Building a Search Engine with Elasticsearch', 'building-search-elasticsearch', 'Learn how to integrate Elasticsearch into your Spring Boot application for powerful full-text search.', '# Elasticsearch Integration\n\nIn this tutorial, we will explore how to use Spring Data Elasticsearch to provide fast and accurate search results for your blog posts.\n\n```java\n@Document(indexName = "posts")\npublic class PostDocument {\n    @Id\n    private String id;\n    private String title;\n    // ...\n}\n```', 'PUBLISHED', 1, 560, 0, 0, 1, 1, NOW(), NOW(), NOW(), 'SYSTEM', 0);

-- 5. Associate tags with posts
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id FROM `blog_post` p, `blog_tag` t WHERE p.slug = 'welcome-to-nexus' AND t.slug = 'devlog';
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id FROM `blog_post` p, `blog_tag` t WHERE p.slug = 'java-21-features' AND t.slug = 'java';
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id FROM `blog_post` p, `blog_tag` t WHERE p.slug = 'modern-frontend-react' AND t.slug = 'react';
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id FROM `blog_post` p, `blog_tag` t WHERE p.slug = 'building-search-elasticsearch' AND t.slug = 'spring-boot';
INSERT IGNORE INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.id, t.id FROM `blog_post` p, `blog_tag` t WHERE p.slug = 'building-search-elasticsearch' AND t.slug = 'java';

-- 6. Add sample moments
INSERT IGNORE INTO `blog_moment` (`content`, `is_published`, `likes_count`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
('Just deployed the new version of Nexus! The Cmd+K search feels so snappy. 🚀', 1, 12, NOW(), NOW(), 'SYSTEM', 0),
('Coffee and code on a Sunday morning. Working on the analytics dashboard today.', 1, 5, NOW(), NOW(), 'SYSTEM', 0),
('Finally finished reading "Clean Code". Highly recommended for any developer!', 1, 8, NOW(), NOW(), 'SYSTEM', 0);

-- 7. Add sample projects
INSERT INTO `blog_project` (
    `name`,
    `description`,
    `github_url`,
    `repo_name`,
    `is_published`,
    `sort_order`,
    `created_at`,
    `updated_at`,
    `created_by`,
    `is_deleted`
)
SELECT
    'Nexus CMS',
    'A modern, lightweight blog and CMS system built with Spring Boot.',
    'https://github.com/yourusername/nexus',
    'nexus',
    1,
    1,
    NOW(),
    NOW(),
    'SYSTEM',
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM `blog_project`
    WHERE `name` = 'Nexus CMS'
       OR `repo_name` = 'nexus'
);

INSERT INTO `blog_project` (
    `name`,
    `description`,
    `github_url`,
    `repo_name`,
    `is_published`,
    `sort_order`,
    `created_at`,
    `updated_at`,
    `created_by`,
    `is_deleted`
)
SELECT
    'Starry Weather',
    'A minimalist weather application with beautiful astronomy-themed backgrounds.',
    'https://github.com/yourusername/starry-weather',
    'starry-weather',
    1,
    2,
    NOW(),
    NOW(),
    'SYSTEM',
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM `blog_project`
    WHERE `name` = 'Starry Weather'
       OR `repo_name` = 'starry-weather'
);
