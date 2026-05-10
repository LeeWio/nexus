CREATE TABLE sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert some default configurations
INSERT INTO sys_config (config_key, config_value, config_name, description, is_public) VALUES
('site_name', 'Nexus Blog', 'Site Name', 'The name of the website', TRUE),
('site_description', 'A modern CMS built with Spring Boot', 'Site Description', 'SEO description', TRUE),
('allow_registration', 'true', 'Allow Registration', 'Whether to allow new users to register', TRUE),
('allow_anonymous_comment', 'false', 'Allow Anonymous Comment', 'Whether to allow comments without login', FALSE);
