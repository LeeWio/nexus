-- Give a fresh or previously empty workspace a usable starting board.
-- Existing boards are left untouched.
INSERT INTO kanban_column (name, color, order_index, created_at, updated_at)
SELECT defaults.name, defaults.color, defaults.order_index, NOW(), NOW()
FROM (
    SELECT 'To Do' AS name, 'accent' AS color, 0 AS order_index
    UNION ALL SELECT 'In Progress', 'warning', 1
    UNION ALL SELECT 'Done', 'success', 2
) defaults
WHERE NOT EXISTS (SELECT 1 FROM kanban_column WHERE is_deleted = FALSE);
