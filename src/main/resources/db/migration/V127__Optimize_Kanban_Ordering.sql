CREATE INDEX idx_kanban_column_order ON kanban_column(order_index, id);
CREATE INDEX idx_kanban_item_column_order
    ON kanban_item(column_id, is_deleted, order_index, id);
