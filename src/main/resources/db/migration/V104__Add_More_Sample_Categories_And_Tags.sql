-- V104: Add more sample categories and tags

-- Add more categories
INSERT IGNORE INTO `blog_category` (`name`, `slug`, `description`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
('Frontend', 'frontend', 'Frontend development, UI/UX, and web technologies.', NOW(), NOW(), 'SYSTEM', 0),
('Backend', 'backend', 'Backend development, servers, and APIs.', NOW(), NOW(), 'SYSTEM', 0),
('DevOps', 'devops', 'Operations, deployments, and CI/CD pipelines.', NOW(), NOW(), 'SYSTEM', 0),
('Artificial Intelligence', 'ai', 'AI, Machine Learning, and Data Science.', NOW(), NOW(), 'SYSTEM', 0),
('Database', 'database', 'Database design, SQL, NoSQL, and caching.', NOW(), NOW(), 'SYSTEM', 0);

-- Add more tags
INSERT IGNORE INTO `blog_tag` (`name`, `slug`, `created_at`, `updated_at`, `created_by`, `is_deleted`)
VALUES 
('Vue.js', 'vuejs', NOW(), NOW(), 'SYSTEM', 0),
('Angular', 'angular', NOW(), NOW(), 'SYSTEM', 0),
('Node.js', 'nodejs', NOW(), NOW(), 'SYSTEM', 0),
('Python', 'python', NOW(), NOW(), 'SYSTEM', 0),
('Docker', 'docker', NOW(), NOW(), 'SYSTEM', 0),
('Kubernetes', 'kubernetes', NOW(), NOW(), 'SYSTEM', 0),
('MySQL', 'mysql', NOW(), NOW(), 'SYSTEM', 0),
('PostgreSQL', 'postgresql', NOW(), NOW(), 'SYSTEM', 0),
('Redis', 'redis', NOW(), NOW(), 'SYSTEM', 0),
('Linux', 'linux', NOW(), NOW(), 'SYSTEM', 0),
('AWS', 'aws', NOW(), NOW(), 'SYSTEM', 0);
