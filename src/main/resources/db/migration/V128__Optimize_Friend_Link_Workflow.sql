CREATE INDEX idx_friend_link_url ON blog_friend_link(url);
CREATE INDEX idx_friend_link_public
    ON blog_friend_link(status, is_published, is_deleted, sort_order, created_at);
