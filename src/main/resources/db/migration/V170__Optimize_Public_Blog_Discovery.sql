-- Cover the two public read paths introduced by the discovery and series APIs.
-- The leading visibility/status columns keep the candidate and column scans
-- selective before applying deterministic ranking/order columns.
CREATE INDEX idx_blog_post_public_discovery
    ON blog_post (is_deleted, status, is_featured, published_at, id);

CREATE INDEX idx_blog_post_public_series
    ON blog_post (is_deleted, series_id, status, series_order, published_at, id);
