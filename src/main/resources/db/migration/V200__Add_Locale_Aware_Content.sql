CREATE TABLE blog_category_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    CONSTRAINT fk_category_translation_category FOREIGN KEY (category_id) REFERENCES blog_category (id),
    CONSTRAINT uk_category_translation_locale UNIQUE (category_id, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_tag_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT fk_tag_translation_tag FOREIGN KEY (tag_id) REFERENCES blog_tag (id),
    CONSTRAINT uk_tag_translation_locale UNIQUE (tag_id, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_life_list_category_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT fk_life_category_translation_category FOREIGN KEY (category_id) REFERENCES blog_life_list_category (id),
    CONSTRAINT uk_life_category_translation_locale UNIQUE (category_id, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_post_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT,
    auto_summary VARCHAR(500),
    toc TEXT,
    CONSTRAINT fk_post_translation_post FOREIGN KEY (post_id) REFERENCES blog_post (id),
    CONSTRAINT uk_post_translation_locale UNIQUE (post_id, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO blog_category_translation (category_id, locale, name, description)
SELECT id, 'zh', name, description FROM blog_category WHERE is_deleted = false;

INSERT INTO blog_category_translation (category_id, locale, name, description)
SELECT id, 'en',
       CASE slug
           WHEN 'technology' THEN 'Technology'
           WHEN 'life' THEN 'Life'
           WHEN 'travel' THEN 'Travel'
           WHEN 'frontend' THEN 'Frontend'
           WHEN 'backend' THEN 'Backend'
           WHEN 'devops' THEN 'DevOps'
           WHEN 'ai' THEN 'Artificial Intelligence'
           WHEN 'database' THEN 'Database'
           ELSE name
       END,
       description
FROM blog_category
WHERE is_deleted = false;

INSERT INTO blog_tag_translation (tag_id, locale, name)
SELECT id, 'zh', name FROM blog_tag WHERE is_deleted = false;

INSERT INTO blog_tag_translation (tag_id, locale, name)
SELECT id, 'en', name FROM blog_tag WHERE is_deleted = false;

INSERT INTO blog_life_list_category_translation (category_id, locale, name)
SELECT id, 'zh', name FROM blog_life_list_category WHERE is_deleted = false;

INSERT INTO blog_life_list_category_translation (category_id, locale, name)
SELECT id, 'en',
       CASE slug
           WHEN 'travel' THEN 'Travel & exploration'
           WHEN 'learning' THEN 'Learning & growth'
           WHEN 'body' THEN 'Movement & body'
           WHEN 'creation' THEN 'Creation & expression'
           WHEN 'daily' THEN 'Life & everyday'
           WHEN 'relationships' THEN 'Relationships'
           WHEN 'courage' THEN 'Courage & challenges'
           WHEN 'uncategorized' THEN 'Uncategorized'
           ELSE name
       END
FROM blog_life_list_category
WHERE is_deleted = false;
