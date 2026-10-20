CREATE TABLE blog_life_list_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_life_list_category_name (name),
    UNIQUE KEY uk_life_list_category_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_life_list_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    item_number INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    motivation TEXT,
    category_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'planned',
    visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    sort_order INT NOT NULL DEFAULT 0,
    completed_at DATE,
    archived_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_life_list_item_number (item_number),
    KEY idx_life_list_item_public (visibility, status, is_deleted, archived_at),
    CONSTRAINT fk_life_list_item_category FOREIGN KEY (category_id) REFERENCES blog_life_list_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_life_list_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    item_id BIGINT NOT NULL,
    occurred_at DATE,
    content TEXT NOT NULL,
    location VARCHAR(160),
    visibility VARCHAR(20) NOT NULL DEFAULT 'private',
    blog_post_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    KEY idx_life_list_memory_item (item_id, is_deleted),
    CONSTRAINT fk_life_list_memory_item FOREIGN KEY (item_id) REFERENCES blog_life_list_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO blog_life_list_category (name, slug, sort_order) VALUES
    ('旅行与探索', 'travel', 10),
    ('学习与成长', 'learning', 20),
    ('运动与身体', 'body', 30),
    ('创作与表达', 'creation', 40),
    ('生活与日常', 'daily', 50),
    ('人际关系', 'relationships', 60),
    ('勇气与挑战', 'courage', 70),
    ('未分类', 'uncategorized', 80);
