CREATE INDEX idx_blog_post_related_category
    ON blog_post (is_deleted, status, category_id, published_at, id);

CREATE INDEX idx_blog_post_related_series
    ON blog_post (is_deleted, status, series_id, published_at, id);

CREATE INDEX idx_blog_post_series_order
    ON blog_post (is_deleted, series_id, status, series_order, id);
