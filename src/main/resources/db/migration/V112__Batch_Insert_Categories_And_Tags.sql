-- V112: Batch Insert User Provided Categories and Tags
-- Category insertion (using IGNORE to avoid duplicates if some already exist)
INSERT IGNORE INTO `blog_category` (`name`, `slug`, `description`, `created_at`, `updated_at`, `version`) VALUES
('Technology', 'technology', 'General technology trends and news', NOW(), NOW(), 0),
('Lifestyle', 'lifestyle', 'Personal lifestyle, habits, and daily life', NOW(), NOW(), 0),
('Career', 'career', 'Professional development and workplace advice', NOW(), NOW(), 0),
('Finance', 'finance', 'Personal finance, money management, and investing', NOW(), NOW(), 0),
('Culture', 'culture', 'Arts, entertainment, and social topics', NOW(), NOW(), 0),
('Education', 'education', 'Academic learning and skill acquisition', NOW(), NOW(), 0),
('Business', 'business', 'Market trends, startups, and corporate world', NOW(), NOW(), 0);

-- Tag insertion
INSERT IGNORE INTO `blog_tag` (`name`, `slug`, `created_at`, `updated_at`, `version`) VALUES
('Web Development', 'web-development', NOW(), NOW(), 0),
('AI & Machine Learning', 'ai-machine-learning', NOW(), NOW(), 0),
('Travel', 'travel', NOW(), NOW(), 0),
('Health & Wellness', 'health-wellness', NOW(), NOW(), 0),
('Productivity', 'productivity', NOW(), NOW(), 0),
('Career Tips', 'career-tips', NOW(), NOW(), 0),
('Design', 'design', NOW(), NOW(), 0),
('UI/UX', 'ui-ux', NOW(), NOW(), 0),
('Photography', 'photography', NOW(), NOW(), 0),
('Personal Finance', 'personal-finance', NOW(), NOW(), 0),
('Investing', 'investing', NOW(), NOW(), 0),
('Book Reviews', 'book-reviews', NOW(), NOW(), 0),
('Film & TV', 'film-tv', NOW(), NOW(), 0),
('Learning Tips', 'learning-tips', NOW(), NOW(), 0),
('Science Explained', 'science-explained', NOW(), NOW(), 0),
('Startups', 'startups', NOW(), NOW(), 0),
('Marketing', 'marketing', NOW(), NOW(), 0);
