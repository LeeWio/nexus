CREATE TABLE IF NOT EXISTS kanban_item_assignee (
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (item_id, user_id),
    KEY idx_kanban_assignee_user (user_id, item_id),
    CONSTRAINT fk_kanban_assignee_item FOREIGN KEY (item_id) REFERENCES kanban_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_kanban_assignee_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
